package nl.tudelft.trustchain.common.bitcoin

import org.bitcoinj.core.*
import org.bitcoinj.crypto.TransactionSignature
import org.bitcoinj.kits.WalletAppKit
import org.bitcoinj.wallet.DeterministicSeed
import org.bitcoinj.wallet.KeyChain
import org.bitcoinj.wallet.SendRequest
import org.bitcoinj.wallet.Wallet
import java.io.File
import java.net.InetAddress

class BitcoinWallet(params: NetworkParameters, seed: String, walletDirectory: File, walletName: String) {
    private val regnetPeerAddress = "87.211.15.72"
    var kit: WalletAppKit
    private lateinit var key: ECKey
    private var retrievedKey = false

    init {
        kit = WalletAppKit(params, walletDirectory, walletName)
        val deterministicSeed = DeterministicSeed(seed, null, "", 0)
        kit = kit.restoreWalletFromSeed(deterministicSeed)
        kit.setPeerNodes(PeerAddress(params, InetAddress.getByName(regnetPeerAddress), params.port))
        kit.startAsync()
    }

    fun send(transaction: Transaction) {
        val sendRequest = SendRequest.forTx(transaction)
        kit.wallet().completeTx(sendRequest)
        kit.wallet().commitTx(transaction)
        kit.peerGroup().broadcastTransaction(transaction)
    }

    fun address(): Address {
        return kit.wallet().currentReceiveAddress()
    }

    fun balance(): Coin {
        return kit.wallet().getBalance(Wallet.BalanceType.AVAILABLE)
    }

    fun sign(transactionHashes: List<Sha256Hash>): List<TransactionSignature> {
        val transactionSignatures = mutableListOf<TransactionSignature>()
        transactionHashes.forEachIndexed { _, transactionHash ->
            val signature = key().sign(transactionHash)
            val transactionSignature = TransactionSignature(signature, Transaction.SigHash.ALL, false)
            transactionSignatures.add(transactionSignature)
        }
        return transactionSignatures
    }

    fun key(): ECKey {
        if (!retrievedKey) {
            key = kit.wallet().currentKey(KeyChain.KeyPurpose.AUTHENTICATION)
            retrievedKey = true
        }
        return key
    }
}
