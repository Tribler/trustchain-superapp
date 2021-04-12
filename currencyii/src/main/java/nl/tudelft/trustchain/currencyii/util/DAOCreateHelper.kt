package nl.tudelft.trustchain.currencyii.util

import nl.tudelft.ipv8.Peer
import nl.tudelft.ipv8.android.IPv8Android
import nl.tudelft.ipv8.attestation.trustchain.TrustChainCommunity
import nl.tudelft.ipv8.util.toHex
import nl.tudelft.trustchain.currencyii.CoinCommunity
import nl.tudelft.trustchain.currencyii.TrustChainHelper
import nl.tudelft.trustchain.currencyii.coin.WalletManagerAndroid
import nl.tudelft.trustchain.currencyii.sharedWallet.SWJoinBlockTransactionData
import org.bitcoinj.core.Coin
import java.util.concurrent.TimeUnit

class DAOCreateHelper() {
    private fun getTrustChainCommunity(): TrustChainCommunity {
        return IPv8Android.getInstance().getOverlay()
            ?: throw IllegalStateException("TrustChainCommunity is not configured")
    }

    private val trustchain: TrustChainHelper by lazy {
        TrustChainHelper(getTrustChainCommunity())
    }

    /**
     * 1.1 Create a shared wallet block.
     * The bitcoin transaction may take some time to finish.
     * If the transaction     private fun getTrustChainCommunity(): TrustChainCommunity {
     return IPv8Android.getInstance().getOverlay()
     ?: throw IllegalStateException("TrustChainCommunity is not configured")
     }

     private val trustchain: TrustChainHelper by lazy {
     TrustChainHelper(getTrustChainCommunity())
     }is valid, the result is broadcasted on trust chain.
     * **Throws** exceptions if something goes wrong with creating or broadcasting bitcoin transaction.
     */
    public fun createBitcoinGenesisWallet(
        myPeer: Peer,
        entranceFee: Long,
        threshold: Int,
        progressCallback: ((progress: Double) -> Unit)? = null,
        timeout: Long = CoinCommunity.DEFAULT_BITCOIN_MAX_TIMEOUT
    ) {
        val walletManager = WalletManagerAndroid.getInstance()
        val transactionBroadcast = walletManager.safeCreationAndSendGenesisWallet(
            Coin.valueOf(entranceFee)
        )

        if (progressCallback != null) {
            transactionBroadcast.setProgressCallback { progress ->
                progressCallback(progress)
            }
        }

        // Try to broadcast the bitcoin transaction.
        val transaction = transactionBroadcast.broadcast().get(timeout, TimeUnit.SECONDS)
        val serializedTransaction = CoinCommunity.getSerializedTransaction(transaction)

        // Broadcast on trust chain if no errors are thrown in the previous step.
        broadcastCreatedSharedWallet(
            myPeer,
            serializedTransaction,
            entranceFee,
            threshold
        )
    }

    /**
     * 1.2 Finishes the last step of creating a genesis shared bitcoin wallet.
     * Posts a self-signed trust chain block containing the shared wallet data.
     */
    private fun broadcastCreatedSharedWallet(
        myPeer: Peer,
        transactionSerialized: String,
        entranceFee: Long,
        votingThreshold: Int
    ) {
        val bitcoinPublicKey = WalletManagerAndroid.getInstance().networkPublicECKeyHex()
        val trustChainPk = myPeer.publicKey.keyToBin()

        val blockData = SWJoinBlockTransactionData(
            entranceFee,
            transactionSerialized,
            votingThreshold,
            arrayListOf(trustChainPk.toHex()),
            arrayListOf(bitcoinPublicKey)
        )

        trustchain.createProposalBlock(blockData.getJsonString(), trustChainPk, blockData.blockType)
    }
}
