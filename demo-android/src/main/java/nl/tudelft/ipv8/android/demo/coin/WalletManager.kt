package nl.tudelft.ipv8.android.demo.coin

import android.util.Log
import com.google.common.util.concurrent.ListenableFuture
import org.bitcoinj.core.*
import org.bitcoinj.core.ECKey.ECDSASignature
import org.bitcoinj.kits.WalletAppKit
import org.bitcoinj.params.TestNet3Params
import org.bitcoinj.script.Script
import org.bitcoinj.script.ScriptBuilder
import org.bitcoinj.script.ScriptPattern
import org.bitcoinj.wallet.SendRequest
import org.bitcoinj.wallet.Wallet
import java.io.File
import java.util.*


/**
 * The wallet manager which encapsulates the functionality of all possible interactions
 * with bitcoin wallets (including multi-signature wallets).
 * NOTE: Ideally should be separated from any Android UI concepts. Not the case currently.
 */
class WalletManager(walletManagerConfiguration: WalletManagerConfiguration, walletDir: File) {
    private val kit: WalletAppKit
    val params: NetworkParameters

    init {
        Log.i("Coin", "Coin: WalletManager attempting to start.")

        params = when (walletManagerConfiguration.network) {
            BitcoinNetworkOptions.TEST_NET -> TestNet3Params.get()
            BitcoinNetworkOptions.PRODUCTION -> TestNet3Params.get()
        }

        val filePrefix = when (walletManagerConfiguration.network) {
            BitcoinNetworkOptions.TEST_NET -> "forwarding-service-testnet"
            BitcoinNetworkOptions.PRODUCTION -> "forwarding-service"
        }

        kit = object : WalletAppKit(params, walletDir, filePrefix) {
            override fun onSetupCompleted() {
                // Make a fresh new key if no keys in stored wallet.
                if (wallet().keyChainGroupSize < 1) wallet().importKey(ECKey())
                Log.i("Coin", "Coin: WalletManager started successfully.")
            }
        }

        kit.startAsync()
    }

    fun createMultiSignatureWallet(ourPublicKey: ECKey, otherPublicKeys: List<ECKey>) {
        // Prepare a template for the contract.
        val contract = Transaction(params)

        // Prepare a list of all keys present in contract.
        val copiedList = otherPublicKeys.toMutableList()
        copiedList.add(ourPublicKey)
        val keys = Collections.unmodifiableList(copiedList)

        // Create a n-n multi-signature output script.
        val script = ScriptBuilder.createMultiSigOutputScript(keys.size, keys)
        // Now add an output with minimum fee needed.
        // TODO: check if this is enough, or find out how we add fees to a transaction.
        val amount: Coin = Transaction.REFERENCE_DEFAULT_MIN_TX_FEE
        contract.addOutput(amount, script)

        // Add the input to the transaction (so the above amount can be sent to the wallet).
        val req = SendRequest.forTx(contract)
        kit.wallet().completeTx(req)

        // Broadcast and wait for it to propagate across the network.
        // It should take a few seconds unless something went wrong.
        val broadcast: ListenableFuture<Transaction> =
            kit.peerGroup().broadcastTransaction(req.tx).broadcast()
        broadcast.addListener(Runnable {
            Log.d("Coin", "Coin: created a multisignature wzllet.")
        }, WalletManagerAndroid.runInUIThread)
    }

    fun signMultiSignatureMessage(
        contract: Transaction,
        myPublicKey: ECKey,
        receiverAddress: ECKey,
        value: Coin
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

    fun getBalance(): Long {
        Log.e("Coin", kit.wallet().getBalance(Wallet.BalanceType.ESTIMATED).toFriendlyString())
        // TODO: Does not show correct value.
        return kit.wallet().getBalance(Wallet.BalanceType.ESTIMATED).value
    }

    fun getImportedKeyPairs(): MutableList<ECKey>? {
        return kit.wallet().importedKeys
    }

    fun importPrivateKey(privateKey: String) {
        kit.wallet().importKey(privateKeyStringToECKey(privateKey))
    }

    fun privateKeyStringToECKey(privateKey: String): ECKey {
        return DumpedPrivateKey.fromBase58(params, privateKey).key
    }

    fun ecKeyToPrivateKeyString(ecKey: ECKey): String {
        return ecKey.getPrivateKeyAsWiF(params)
    }

}
