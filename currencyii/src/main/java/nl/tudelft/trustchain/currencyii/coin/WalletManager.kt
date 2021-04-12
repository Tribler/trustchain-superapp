package nl.tudelft.trustchain.currencyii.coin

import android.util.Log
import com.google.common.base.Joiner
import com.google.gson.JsonParser
import info.blockchain.api.APIException
import info.blockchain.api.blockexplorer.BlockExplorer
import nl.tudelft.ipv8.util.hexToBytes
import nl.tudelft.ipv8.util.toHex
import org.bitcoinj.core.*
import org.bitcoinj.core.DumpedPrivateKey
import org.bitcoinj.core.ECKey.ECDSASignature
import org.bitcoinj.core.LegacyAddress
import org.bitcoinj.core.listeners.DownloadProgressTracker
import org.bitcoinj.crypto.TransactionSignature
import org.bitcoinj.kits.WalletAppKit
import org.bitcoinj.params.MainNetParams
import org.bitcoinj.params.TestNet3Params
import org.bitcoinj.script.Script
import org.bitcoinj.script.ScriptBuilder
import org.bitcoinj.script.ScriptPattern
import org.bitcoinj.wallet.DeterministicSeed
import org.bitcoinj.wallet.KeyChainGroup
import org.bitcoinj.wallet.KeyChainGroupStructure
import org.bitcoinj.wallet.SendRequest
import java.io.File
import java.util.*

const val TEST_NET_WALLET_NAME = "forwarding-service-testnet"
const val MAIN_NET_WALLET_NAME = "forwarding-service"
const val MIN_BLOCKCHAIN_PEERS = 5

/**
 * The wallet manager which encapsulates the functionality of all possible interactions
 * with bitcoin wallets (including multi-signature wallets).
 * NOTE: Ideally should be separated from any Android UI concepts. Not the case currently.
 */
@Suppress("DEPRECATION", "NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS")
class WalletManager(
    walletManagerConfiguration: WalletManagerConfiguration,
    walletDir: File,
    serializedDeterministicKey: SerializedDeterministicKey? = null,
    addressPrivateKeyPair: AddressPrivateKeyPair? = null
) {
    val kit: WalletAppKit
    val params: NetworkParameters
    var isDownloading: Boolean = true
    var progress: Int = 0

    /**
     * Initializes WalletManager.
     */
    init {
        Log.i("Coin", "Coin: WalletManager attempting to start.")

        params = when (walletManagerConfiguration.network) {
            BitcoinNetworkOptions.TEST_NET -> TestNet3Params.get()
            BitcoinNetworkOptions.PRODUCTION -> MainNetParams.get()
        }

        val filePrefix = when (walletManagerConfiguration.network) {
            BitcoinNetworkOptions.TEST_NET -> TEST_NET_WALLET_NAME
            BitcoinNetworkOptions.PRODUCTION -> MAIN_NET_WALLET_NAME
        }

        kit = object : WalletAppKit(params, walletDir, filePrefix) {
            override fun onSetupCompleted() {
                // Make a fresh new key if no keys in stored wallet.
                if (wallet().keyChainGroupSize < 1) {
                    Log.i("Coin", "Coin: Added manually created fresh key")
                    wallet().importKey(ECKey())
                }
                wallet().allowSpendingUnconfirmedTransactions()
                Log.i("Coin", "Coin: WalletManager started successfully.")
            }
        }

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

        kit.setDownloadListener(object : DownloadProgressTracker() {
            override fun progress(
                pct: Double,
                blocksSoFar: Int,
                date: Date?
            ) {
                super.progress(pct, blocksSoFar, date)
                val percentage = pct.toInt()
                progress = percentage
                println("Progress: $percentage")
                Log.i("Coin", "Progress: $percentage")
            }

            override fun doneDownload() {
                super.doneDownload()
                progress = 100
                Log.i("Coin", "Download Complete!")
                Log.i("Coin", "Balance: ${kit.wallet().balance}")
                isDownloading = false
            }
        })

        Log.i("Coin", "Coin: starting the setup of kit.")
        kit.setBlockingStartup(false)
            .startAsync()
            .awaitRunning()

        if (addressPrivateKeyPair != null) {
            Log.i(
                "Coin",
                "Coin: Importing Address: ${addressPrivateKeyPair.address}, " +
                    "with SK: ${addressPrivateKeyPair.privateKey}"
            )

            val privateKey = addressPrivateKeyPair.privateKey
            val key = formatKey(privateKey)

            Log.i(
                "Coin",
                "Coin: Address from private key is: " + LegacyAddress.fromKey(
                    params,
                    key
                ).toString()
            )

            kit.wallet().importKey(key)
        }
        Log.i("Coin", "Coin: finished the setup of kit.")
        Log.i("Coin", "Coin: Imported Keys: ${kit.wallet().importedKeys}")
        Log.i("Coin", "Coin: Imported Keys: ${kit.wallet().toString(true, false, false, null)}")
    }

    fun formatKey(privateKey: String): ECKey {
        return if (privateKey.length == 51 || privateKey.length == 52) {
            val dumpedPrivateKey =
                DumpedPrivateKey.fromBase58(params, privateKey)
            dumpedPrivateKey.key
        } else {
            val bigIntegerPrivateKey = Base58.decodeToBigInteger(privateKey)
            ECKey.fromPrivate(bigIntegerPrivateKey)
        }
    }

    /**
     * Returns our bitcoin address we use in all multi-sig contracts
     * we are part of.
     * @return hex representation of our address
     */
    fun protocolAddress(): Address {
        return kit.wallet().issuedReceiveAddresses[0]
    }

    /**
     * Returns our bitcoin public key we use in all multi-sig contracts
     * we are part of.
     * @return hex representation of our public key (this is not an address)
     */
    fun protocolECKey(): ECKey {
        return kit.wallet().issuedReceiveKeys[0]
    }

    /**
     * Returns our bitcoin public key (in hex) we use in all multi-sig contracts
     * we are part of.
     * @return hex representation of our public key (this is not an address)
     */
    fun networkPublicECKeyHex(): String {
        return protocolECKey().publicKeyAsHex
    }

    /**
     * (1) When you are creating a multi-sig wallet for yourself alone
     * as the genesis (wallet).
     * @param entranceFee the entrance fee you are sending
     * @return TransactionPackage
     */
    fun safeCreationAndSendGenesisWallet(
        entranceFee: Coin
    ): TransactionBroadcast {
        Log.i("Coin", "Coin: (safeCreationAndSendGenesisWallet start).")

        Log.i("Coin", "Coin: we are making a new genesis wallet for us alone.")
        val keys = listOf(ECKey.fromPublicOnly(networkPublicECKeyHex().hexToBytes()))
        val threshold = 1

        Log.i("Coin", "Coin: we will now make a ${keys.size}/$threshold wallet")
        val transaction = Transaction(params)

        // Create the locking multi-sig script for the output.
        val script = ScriptBuilder.createMultiSigOutputScript(threshold, keys)

        // Add an output with the entrance fee & script.
        transaction.addOutput(entranceFee, script)

        Log.i("Coin", "Coin: use SendRequest to add our entranceFee input & change address.")
        val req = SendRequest.forTx(transaction)
        req.changeAddress = protocolAddress()
        kit.wallet().completeTx(req)

        return sendTransaction(req.tx)
    }

    /**
     * (2) Use this when you want to join an /existing/ wallet.
     * You need to broadcast this transaction to all the old owners so they can sign it.
     * @param networkPublicHexKeys list of NEW wallet owners
     * @param entranceFee the entrance fee
     * @param oldTransaction the old transaction
     * @param newThreshold the new threshold (default to # of new owners)
     * @return the resulting transaction (unsigned multi-sig input!)
     */
    fun safeCreationJoinWalletTransaction(
        networkPublicHexKeys: List<String>,
        entranceFee: Coin,
        oldTransaction: Transaction,
        newThreshold: Int = networkPublicHexKeys.size
    ): TransactionPackage {
        Log.i("Coin", "Coin: (safeCreationJoinWalletTransaction start).")

        Log.i("Coin", "Coin: making a transaction with you in it for everyone to sign.")
        val newTransaction = Transaction(params)
        val oldMultiSignatureOutput = getMultiSigOutput(oldTransaction).unsignedOutput

        Log.i("Coin", "Coin: output (1) -> we are adding the final new multi-sig output.")
        val newKeys = networkPublicHexKeys.map { publicHexKey: String ->
            Log.i("Coin", "Coin: de-serializing key $publicHexKey.")
            ECKey.fromPublicOnly(publicHexKey.hexToBytes())
        }
        val newMultiSignatureScript =
            ScriptBuilder.createMultiSigOutputScript(newThreshold, newKeys)

        // Calculate the final amount of coins (old coins + entrance fee) that will be the new multi-sig.
        val newMultiSignatureOutputMoney = oldMultiSignatureOutput.value.add(entranceFee)
        newTransaction.addOutput(newMultiSignatureOutputMoney, newMultiSignatureScript)

        Log.i("Coin", "Coin: input (1) -> we are adding the old multi-sig as input.")
        val multiSignatureInput = newTransaction.addInput(oldMultiSignatureOutput)
        // Disconnecting, because we will supply our own script_sig later (in signing process).
        multiSignatureInput.disconnect()

        Log.i("Coin", "Coin: use SendRequest to add our entranceFee inputs & change address.")
        val req = SendRequest.forTx(newTransaction)
        printTransactionInformation(req.tx)
        req.changeAddress = protocolAddress()
        kit.wallet().completeTx(req)

        return TransactionPackage(
            req.tx.txId.toString(),
            req.tx.bitcoinSerialize().toHex()
        )
    }

    /**
     * (2.1) You are (part) owner of a wallet a proposer wants to join. Sign the new wallet
     * and send it back to the proposer.
     * @param newTransaction the new transaction
     * @param oldTransaction the old transaction
     * @param key the key that will be signed with
     * @return the signature (you need to send back)
     */
    fun safeSigningJoinWalletTransaction(
        newTransaction: Transaction,
        oldTransaction: Transaction,
        key: ECKey
    ): ECDSASignature {
        Log.i("Coin", "Coin: (safeSigningJoinWalletTransaction start).")

        val oldMultiSignatureOutput = getMultiSigOutput(oldTransaction).unsignedOutput
        val sighash: Sha256Hash = newTransaction.hashForSignature(
            0,
            oldMultiSignatureOutput.scriptPubKey,
            Transaction.SigHash.ALL,
            false
        )
        val signature: ECDSASignature = key.sign(sighash)
        Log.i("Coin", "Coin: key -> ${key.publicKeyAsHex}")
        Log.i("Coin", "Coin: signature -> ${signature.encodeToDER().toHex()}")
        return signature
    }

    /**
     * (2.2) You are the proposer. You have collected the needed signatures and
     * will make the final transaction.
     * @param signaturesOfOldOwners signatures (of the OLD owners only, in correct order)
     * @param newTransaction SendRequest
     * @param oldTransaction Transaction
     * @return TransactionPackage?
     */
    fun safeSendingJoinWalletTransaction(
        signaturesOfOldOwners: List<ECDSASignature>,
        newTransaction: Transaction,
        oldTransaction: Transaction
    ): TransactionBroadcast {
        Log.i("Coin", "Coin: (safeSendingJoinWalletTransaction start).")
        val oldMultiSigOutput = getMultiSigOutput(oldTransaction).unsignedOutput

        Log.i("Coin", "Coin: make the new final transaction for the new wallet.")
        Log.i("Coin", "Coin: using ${signaturesOfOldOwners.size} signatures.")
        val transactionSignatures = signaturesOfOldOwners.map { signature ->
            TransactionSignature(signature, Transaction.SigHash.ALL, false)
        }
        val inputScript = ScriptBuilder.createMultiSigInputScript(transactionSignatures)

        // TODO: see if it is a issue to always assume the 1st input is the multi-sig input.
        val newMultiSigInput = newTransaction.inputs[0]
        newMultiSigInput.scriptSig = inputScript

        // Verify the script before sending.
        try {
            newMultiSigInput.verify(oldMultiSigOutput)
            Log.i("Coin", "Coin: script is valid.")
        } catch (exception: VerificationException) {
            Log.i("Coin", "Coin: script is NOT valid. ${exception.message}.")
            throw IllegalStateException("The join bitcoin DAO transaction script is invalid")
        }

        return sendTransaction(newTransaction)
    }

    /**
     * (3.1) There is a set-up multi-sig wallet and a proposal, create a signature
     * for the proposal.
     * The transaction includes an output for residual funds using calculated fee estimates.
     * @param transaction PREVIOUS transaction with the multi-sig output
     * @param myPrivateKey key to sign with (yourself most likely)
     * @param receiverAddress receiver address
     * @param paymentAmount amount for receiver address
     * @return ECDSASignature
     */
    fun safeSigningTransactionFromMultiSig(
        transaction: Transaction,
        myPrivateKey: ECKey,
        receiverAddress: Address,
        paymentAmount: Coin
    ): ECDSASignature {
        Log.i("Coin", "Coin: (safeSigningTransactionFromMultiSig start).")

        Log.i("Coin", "Coin: a transaction will be signed from one of our multi-sig outputs.")
        // Retrieve the multi-signature contract.
        val previousMultiSigOutput: TransactionOutput =
            getMultiSigOutput(transaction).unsignedOutput

        // Create the transaction which will have the multisig output as input,
        // The outputs will be the receiver address and another one for residual funds
        val spendTx =
            createMultiSigPaymentTx(receiverAddress, paymentAmount, previousMultiSigOutput)

        // Sign the transaction and return it.
        val multiSigScript = previousMultiSigOutput.scriptPubKey
        val sighash: Sha256Hash =
            spendTx.hashForSignature(0, multiSigScript, Transaction.SigHash.ALL, false)
        val signature: ECDSASignature = myPrivateKey.sign(sighash)

        return signature
    }

    /**
     * (3.2) There is a set-up multi-sig wallet and there are enough signatures
     * to broadcast a transaction with.
     * The transaction includes an output for residual funds using calculated fee estimates.
     * @param transaction transaction with multi-sig output.
     * @param signatures signatures of owners (yourself included)
     * @param receiverAddress receiver address
     * @param paymentAmount amount for receiver address
     * @return transaction
     */
    fun safeSendingTransactionFromMultiSig(
        transaction: Transaction,
        signatures: List<ECDSASignature>,
        receiverAddress: Address,
        paymentAmount: Coin
    ): TransactionBroadcast {
        Log.i("Coin", "Coin: (safeSendingTransactionFromMultiSig start).")

        // Retrieve the multi-sig output. Will become the input of this tx
        val previousMultiSigOutput: TransactionOutput =
            getMultiSigOutput(transaction).unsignedOutput

        Log.i("Coin", "Coin: creating the input script to unlock the multi-sig input.")
        // Create the script that combines the signatures (to spend the multi-signature output).
        val transactionSignatures = signatures.map { signature ->
            TransactionSignature(signature, Transaction.SigHash.ALL, false)
        }
        val inputScript = ScriptBuilder.createMultiSigInputScript(transactionSignatures)

        // Create the transaction which will sign the previous multisig output and use it as input
        // The outputs will be the receiver address and another one for residual funds
        val spendTx = createMultiSigPaymentTxWithInputSig(
            receiverAddress,
            paymentAmount,
            previousMultiSigOutput,
            inputScript
        )

        // We assume a multisig payment transaction only has the multisig as input here, be careful!
        val input = spendTx.inputs[0]

        // Verify the script before sending.
        input.verify(previousMultiSigOutput)
        Log.i("Coin", "Coin: script is valid.")

        return sendTransaction(spendTx)
    }

    /**
     * Helper method to send transaction with logs and progress logs.
     * @param transaction transaction
     */
    private fun sendTransaction(transaction: Transaction): TransactionBroadcast {
        Log.i("Coin", "Coin: (sendTransaction start).")
        Log.i("Coin", "Coin: txId: ${transaction.txId}")
        printTransactionInformation(transaction)

        Log.i("Coin", "Waiting for peers")
        kit.peerGroup().waitForPeers(MIN_BLOCKCHAIN_PEERS).get()
        Log.i("Coin", "Got >= $MIN_BLOCKCHAIN_PEERS peers: ${kit.peerGroup().connectedPeers}")
        Log.i(
            "Coin",
            "Transaction broadcast setup ${transaction.txId} completed. Not broadcasted yet."
        )
        return kit.peerGroup().broadcastTransaction(transaction)

//        broadcastTransaction.setProgressCallback { progress ->
//            Log.i("Coin", "Coin: broadcast of transaction ${transaction.txId} progress: $progress.")
//        }
//        Log.i("Coin", "Coin: transaction broadcast of ${transaction.txId} is initiated.")
//        broadcastTransaction.broadcast()
    }

    /**
     * Helper method to get the multi-sig output from a transaction.
     * NOTE: make sure that there is an actual multi-sig output!
     * @param transaction transaction with multi-sig output.
     * @return the multi-sig output
     */
    fun getMultiSigOutput(transaction: Transaction): MultiSigOutputMeta {
        val multiSigOutputs = mutableListOf<TransactionOutput>()
        transaction.outputs.forEach { output ->
            if (ScriptPattern.isSentToMultisig(output.scriptPubKey)) {
                multiSigOutputs.add(output)
            }
        }

        if (multiSigOutputs.size != 1) {
            Log.i("Coin", "Coin: (getMultiSigOutput) the multi-sig output not available.")
        }

        val multiSigOutput = multiSigOutputs[0]

        return MultiSigOutputMeta(
            multiSigOutput.value,
            multiSigOutput.scriptPubKey.pubKeys,
            multiSigOutput.index,
            multiSigOutput.scriptPubKey.numberOfSignaturesRequiredToSpend,
            multiSigOutput
        )
    }

    /**
     * Helper method to attempt to get the transaction from a transaction ID
     * and return serialized version.
     * @param transactionId transactionId
     * @return null if not available in your wallet yet, else serialized version of transaction.
     */
    fun attemptToGetTransactionAndSerialize(transactionId: String): String? {
        val transaction = kit.wallet().getTransaction(Sha256Hash.wrap(transactionId))
        if (transaction != null) {
            Log.i("Coin", "Transaction (attemptToGetTransactionAndSerialize) $transaction found")
            val serializedTransaction = transaction.bitcoinSerialize().toHex()
            return serializedTransaction
        } else {
            Log.i(
                "Coin",
                "Coin: (attemptToGetTransactionAndSerialize) " +
                    "the transaction $transaction could not be found in your wallet."
            )
            return null
        }
    }

    /**
     * Helper method that prints useful information about a transaction.
     * @param transaction Transaction
     */
    fun printTransactionInformation(transaction: Transaction) {
        Log.i("Coin", "Coin: ============ Transaction Information ===============")
        Log.i("Coin", "Coin: txId ${transaction.txId}")
        Log.i("Coin", "Coin: fee ${transaction.fee}")
        Log.i("Coin", "Coin: inputs:::")
        transaction.inputs.forEach {
            Log.i("Coin", "Coin:    index ${it.index}")
            Log.i("Coin", "Coin:    value ${it.value}")
            Log.i("Coin", "Coin:    multi-sig ${ScriptPattern.isSentToMultisig(it.scriptSig)}")
        }
        Log.i("Coin", "Coin: outputs:::")
        transaction.outputs.forEach {
            Log.i("Coin", "Coin:    index ${it.index}")
            Log.i("Coin", "Coin:    value ${it.value}")
            Log.i("Coin", "Coin:    multi-sig ${ScriptPattern.isSentToMultisig(it.scriptPubKey)}")
        }
        Log.i("Coin", "Coin: multi-sig output::")
        val a = getMultiSigOutput(transaction)
        a.owners.forEach {
            Log.i("Coin", "Coin: key -> ${it.publicKeyAsHex}")
        }
        Log.i("Coin", "Coin:    # needed -> ${a.threshold}")
        Log.i("Coin", "Coin:    value -> ${a.value}")
        Log.i("Coin", "Coin: ============ Transaction Information ===============")
    }

    /**
     * Creates a MultiSig payment transaction
     *
     * The two outputs of this transaction will be one to the receiver address (the payment) and
     *  one for the residual funds back to the MultiSig address.
     * Uses a previously created MultiSig output as the input of this transaction.
     * Optionally, the input MultiSig can be signed using the inputScriptSig field.
     *
     * @param receiverAddress: the receiver of the payment
     * @param paymentAmount: the amount to be transferred/payed to the receiver
     * @param previousMultiSigOutput: the MultiSig output of the shared wallet, used as new input
     * @param inputScriptSig: (Optional) the input script (ScriptBuilder.createMultiSigInputScript)
     *  created using signatures (TransactionSignature) of a transaction made with this method.
     */
    private fun createMultiSigPaymentTransaction(
        receiverAddress: Address,
        paymentAmount: Coin,
        previousMultiSigOutput: TransactionOutput,
        inputScriptSig: Script? = null
    ): Transaction {
        // Get multisig script of previous output, which will become the input of this tx
        val multiSigScript = previousMultiSigOutput.scriptPubKey

        Log.i("Coin", "Coin: making the transaction (again) that will be sent.")
        val spendTx = Transaction(params)
        spendTx.addOutput(paymentAmount, receiverAddress)
        // Use a placeholder value for the residual output. Size of Tx needs to be accurate to estimate fee.
        val tempResidualOutput = spendTx.addOutput(Coin.valueOf(9999), multiSigScript)
        // Be careful with adding more inputs!! We assume the first input is the multisig input
        val input = spendTx.addInput(previousMultiSigOutput)

        // Calculate fee and set the change output corresponding to calculated fee
        val calculatedFeeValue = CoinUtil.calculateEstimatedTransactionFee(
            spendTx,
            params,
            CoinUtil.TxPriority.LOW_PRIORITY
        )
        // Make sure that the fee does not exceed the amount of funds available
        val calculatedFee =
            Coin.valueOf(calculatedFeeValue.coerceAtMost((previousMultiSigOutput.value - paymentAmount).value))
        val residualFunds = previousMultiSigOutput.value - paymentAmount - calculatedFee
        Log.i(
            "Coin",
            "Coin: Setting output for residual funds ${residualFunds.value} based on a calculated fee of $calculatedFee satoshi."
        )
        tempResidualOutput.value = residualFunds

        // Set input script signatures if passed to the method
        if (inputScriptSig != null) {
            input.scriptSig = inputScriptSig
        }

        return spendTx
    }

    /**
     * Wrapper for creating a MultiSig payment transaction without an input signature
     *
     * @param receiverAddress: the receiver of the payment
     * @param paymentAmount: the amount to be transferred/payed to the receiver
     * @param previousMultiSigOutput: the MultiSig output of the shared wallet, used as new input
     */
    fun createMultiSigPaymentTx(
        receiverAddress: Address,
        paymentAmount: Coin,
        previousMultiSigOutput: TransactionOutput
    ): Transaction {
        return createMultiSigPaymentTransaction(
            receiverAddress,
            paymentAmount,
            previousMultiSigOutput
        )
    }

    /**
     * Wrapper for creating a MultiSig payment transaction with input signature
     *
     * @param receiverAddress: the receiver of the payment
     * @param paymentAmount: the amount to be transferred/payed to the receiver
     * @param previousMultiSigOutput: the MultiSig output of the shared wallet, used as new input
     * @param inputScriptSig: (Optional) the input script (ScriptBuilder.createMultiSigInputScript)
     *  created using signatures (TransactionSignature) of a transaction made with this method.
     */
    fun createMultiSigPaymentTxWithInputSig(
        receiverAddress: Address,
        paymentAmount: Coin,
        previousMultiSigOutput: TransactionOutput,
        inputScriptSig: Script
    ): Transaction {
        return createMultiSigPaymentTransaction(
            receiverAddress,
            paymentAmount,
            previousMultiSigOutput,
            inputScriptSig
        )
    }

    companion object {
        fun createMultiSignatureWallet(
            publicKeys: List<ECKey>,
            entranceFee: Coin,
            threshold: Int,
            params: NetworkParameters = MainNetParams.get()
        ): Transaction {
            // Prepare a template for the contract.
            val contract = Transaction(params)

            // Prepare a list of all keys present in contract.
            val keys = Collections.unmodifiableList(publicKeys)

            // Create a n-n multi-signature output script.
            val script = ScriptBuilder.createMultiSigOutputScript(threshold, keys)

            // Now add an output with the entrance fee & script.
            contract.addOutput(entranceFee, script)

            return contract
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
            } catch (e: APIException) {
                // Return false if API Exception is thrown with reason "Transaction not found: (...)"
                // The library does not support accessing the reason, so use JSON parser to parse message to a JSON object
                if (e.message !== null) {
                    val reason = JsonParser().parse(e.message).asJsonObject.get("reason")
                    if (reason != null) {
                        if (reason.asString.startsWith("Transaction not found: ")) {
                            return false
                        }
                    }
                }

                // API Exception was something other than transaction not found, so still throw it
                throw e
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

        /**
         * A method to create a serialized seed for use in BitcoinJ.
         * @param paramsRaw BitcoinNetworkOptions
         * @return SerializedDeterministicKey
         */
        fun generateRandomDeterministicSeed(paramsRaw: BitcoinNetworkOptions): SerializedDeterministicKey {
            val params = when (paramsRaw) {
                BitcoinNetworkOptions.TEST_NET -> TestNet3Params.get()
                BitcoinNetworkOptions.PRODUCTION -> MainNetParams.get()
            }
            val keyChainGroup = KeyChainGroup.builder(params, KeyChainGroupStructure.DEFAULT)
                .fromRandom(Script.ScriptType.P2PKH).build()
            return deterministicSeedToSerializedDeterministicKey(keyChainGroup.activeKeyChain.seed!!)
        }

        fun deterministicSeedToSerializedDeterministicKey(seed: DeterministicSeed): SerializedDeterministicKey {
            val words = Joiner.on(" ").join(seed.mnemonicCode)
            val creationTime = seed.creationTimeSeconds
            return SerializedDeterministicKey(words, creationTime)
        }
    }

    fun toSeed(): SerializedDeterministicKey {
        val seed = kit.wallet().keyChainSeed
        return deterministicSeedToSerializedDeterministicKey(seed)
    }

    fun addKey(privateKey: String) {
        Log.i("Coin", "Coin: Importing key in existing wallet: $privateKey")
        this.kit.wallet().importKey(formatKey(privateKey))
    }

    data class TransactionPackage(
        val transactionId: String,
        val serializedTransaction: String
    )

    data class MultiSigOutputMeta(
        val value: Coin,
        val owners: MutableList<ECKey>,
        val index: Int,
        val threshold: Int,
        val unsignedOutput: TransactionOutput
    )
}
