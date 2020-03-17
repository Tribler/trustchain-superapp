package nl.tudelft.ipv8.android.demo.coin

import android.util.Log
import com.google.common.base.Joiner
import info.blockchain.api.blockexplorer.BlockExplorer
import org.bitcoinj.core.*
import org.bitcoinj.core.ECKey.ECDSASignature
import org.bitcoinj.core.listeners.DownloadProgressTracker
import org.bitcoinj.crypto.TransactionSignature
import org.bitcoinj.kits.WalletAppKit
import org.bitcoinj.params.MainNetParams
import org.bitcoinj.params.TestNet3Params
import org.bitcoinj.script.Script
import org.bitcoinj.script.ScriptBuilder
import org.bitcoinj.script.ScriptPattern
import org.bitcoinj.wallet.DeterministicSeed
import java.io.File
import java.util.*


/**
 * The wallet manager which encapsulates the functionality of all possible interactions
 * with bitcoin wallets (including multi-signature wallets).
 * NOTE: Ideally should be separated from any Android UI concepts. Not the case currently.
 */
class WalletManager(
    walletManagerConfiguration: WalletManagerConfiguration,
    walletDir: File,
    serializedDeterministicKey: SerializedDeterminsticKey? = null
) {
    val kit: WalletAppKit
    val params: NetworkParameters

    init {
        Log.i("Coin", "Coin: WalletManager attempting to start.")

        params = when (walletManagerConfiguration.network) {
            BitcoinNetworkOptions.TEST_NET -> TestNet3Params.get()
            BitcoinNetworkOptions.PRODUCTION -> MainNetParams.get()
        }

        val filePrefix = when (walletManagerConfiguration.network) {
            BitcoinNetworkOptions.TEST_NET -> "forwarding-service-testnet"
            BitcoinNetworkOptions.PRODUCTION -> "forwarding-service"
        }

        kit = object : WalletAppKit(params, walletDir, filePrefix) {
            override fun onSetupCompleted() {
                // Make a fresh new key if no keys in stored wallet.
                if (wallet().keyChainGroupSize < 1) wallet().importKey(ECKey())
                wallet().allowSpendingUnconfirmedTransactions()
                Log.i("Coin", "Coin: WalletManager started successfully.")
            }
        }

        kit.setDownloadListener(object : DownloadProgressTracker() {
            override fun progress(
                pct: Double,
                blocksSoFar: Int,
                date: Date?
            ) {
                super.progress(pct, blocksSoFar, date)
                val percentage = pct.toInt()
                println("Progress: $percentage")
                Log.i("Coin", "Progress: $percentage")
            }

            override fun doneDownload() {
                super.doneDownload()
                Log.w("Coin", "Download Complete!")
                Log.i("Coin", "Balance: ${kit.wallet().balance}")
            }
        })

        if (serializedDeterministicKey != null) {
            Log.i(
                "Coin",
                "Coin: received a key to import, will clear the wallet and download again."
            )
            val deterministicSeed = DeterministicSeed(
                serializedDeterministicKey.seed,
                null,
                "",
                serializedDeterministicKey.creationTime
            )
            kit.restoreWalletFromSeed(deterministicSeed)
        }

        Log.i("Coin", "Coin: starting the setup of kit.")
        kit.setBlockingStartup(false)
        kit.startAsync()
        kit.awaitRunning()
        Log.i("Coin", "Coin: finished the setup of kit.")

        Log.i("Coin", "Coin: ${kit.wallet()}")
    }

    companion object {
        fun createMultiSignatureWallet(
            publicKeys: List<ECKey>,
            threshold: Int,
            params: NetworkParameters = MainNetParams.get()
        ): Transaction {
            // Prepare a template for the contract.
            val contract = Transaction(params)

            // Prepare a list of all keys present in contract.
            val keys = Collections.unmodifiableList(publicKeys)

            // Create a n-n multi-signature output script.
            val script = ScriptBuilder.createMultiSigOutputScript(threshold, keys)

            // Now add an output with minimum fee needed.
            val amount: Coin = Transaction.REFERENCE_DEFAULT_MIN_TX_FEE
            contract.addOutput(amount, script)

            return contract
        }

        fun signMultiSignatureMessage(
            contract: Transaction,
            myPublicKey: ECKey,
            receiverAddress: ECKey,
            value: Coin,
            params: NetworkParameters = MainNetParams.get()
        ): ECDSASignature {
            // Retrieve the multisignature contract.
            val multisigOutput: TransactionOutput = contract.getOutput(0)
            val multisigScript: Script = multisigOutput.scriptPubKey

            // Validate whether the transaction (= contract) is what we expect.
            if (!ScriptPattern.isSentToMultisig(multisigScript)) {
                throw Exception("Contract is not a multi signature contract!")
            }

            // Build the transaction we want to sign.
            // todo: add validation to check for this value
            // todo: add fees (so we get chosen earlier by miners)
            val value: Coin = value
            val spendTx = Transaction(params)
            spendTx.addOutput(value, receiverAddress)
            spendTx.addInput(multisigOutput)

            // Sign the transaction and return it.
            val sighash: Sha256Hash =
                spendTx.hashForSignature(0, multisigScript, Transaction.SigHash.ALL, false)
            val signature: ECDSASignature = myPublicKey.sign(sighash)

            return signature
        }

        fun checkEntranceFeeTransaction(
            userBitcoinPk: Address,
            bitcoinTransactionHash: Sha256Hash,
            sharedWalletBitcoinPk: Address,
            entranceFee: Double
        ): Boolean {
            // Get transaction from tx hash
            val blockExplorer = BlockExplorer()
            val tx = try {
                blockExplorer.getTransaction(bitcoinTransactionHash.toString())
            } catch (e: Exception) {
                e.printStackTrace()
                return false
            }

            // Check block confirmations
            val blockHeightRelative = blockExplorer.latestBlock.height - tx.blockHeight
            if (blockHeightRelative < 6) {
                println("Transaction was not confirmed by at least 6 blocks:  $blockHeightRelative")
                return false
            }
            if (tx.blockHeight < 0) {
                println("Transaction does not have a valid block height: ${tx.blockHeight}")
                return false
            }

            // Check transaction inputs
            val userBitcoinPkString = userBitcoinPk.toString()
            var hasCorrectInput = false
            for (input in tx.inputs) {
                val inputValue = input.previousOutput.value.toDouble() / 100000000
                if (userBitcoinPkString.equals(input.previousOutput.address) &&
                    inputValue >= entranceFee
                ) {
                    hasCorrectInput = true
                    break
                }
            }

            if (!hasCorrectInput) {
                println("Transaction did not have correct inputs")
                return false
            }

            // Check transaction outputs
            val sharedWalletBitcoinPkString = sharedWalletBitcoinPk.toString()
            var hasCorrectOutput = false
            for (output in tx.outputs) {
                val outputValue = output.value.toDouble() / 100000000
                if (sharedWalletBitcoinPkString.equals(output.address) &&
                    outputValue >= entranceFee
                ) {
                    hasCorrectOutput = true
                    break
                }
            }

            if (!hasCorrectOutput) {
                println("Transaction did not have correct outputs")
                return false
            }

            return true
        }

        fun privateKeyStringToECKey(
            privateKey: String,
            params: NetworkParameters = MainNetParams.get()
        ): ECKey {
            return DumpedPrivateKey.fromBase58(params, privateKey).key
        }

        fun ecKeyToPrivateKeyString(
            ecKey: ECKey,
            params: NetworkParameters = MainNetParams.get()
        ): String {
            return ecKey.getPrivateKeyAsWiF(params)
        }

    }

    /**
     * Contract: multi signature contract in question
     * Signatures: all signatures needed (also includes yourself).
     * Receiver address: the address we are sending to
     * Value: the amount of money we are sending
     */
    fun sendMultiSignatureMessage(
        contract: Transaction,
        signatures: List<ECDSASignature>,
        receiverAddress: ECKey,
        value: Coin
    ) {
        // Make the transaction we want to perform.
        val multisigOutput: TransactionOutput = contract.getOutput(0)
        val spendTx = Transaction(params)
        spendTx.addOutput(value, receiverAddress)
        val input = spendTx.addInput(multisigOutput)

        // Create the script that combines the signatures (to spend the multi-signature output).
        val transactionSignatures = signatures.map { signature ->
            TransactionSignature(signature, Transaction.SigHash.ALL, false)
        }
        val inputScript = ScriptBuilder.createMultiSigInputScript(transactionSignatures)

        // Add it to the input.
        input.scriptSig = inputScript

        // Verify.
        input.verify(multisigOutput)

        // Todo: add listener for when there is completion
        kit.peerGroup().broadcastTransaction(spendTx)
    }

    fun toSeed(): SerializedDeterminsticKey {
        val seed = kit.wallet().keyChainSeed
        val words = Joiner.on(" ").join(seed.mnemonicCode)
        val creationTime = seed.creationTimeSeconds
        Log.i("Coin", "Seed words are: " + words)
        Log.i("Coin", "Seed birthday is: " + creationTime)
        return SerializedDeterminsticKey(words, creationTime)
    }

}
