package nl.tudelft.trustchain.currencyii.util

import android.content.Context
import nl.tudelft.ipv8.Peer
import nl.tudelft.ipv8.android.IPv8Android
import nl.tudelft.ipv8.attestation.trustchain.TrustChainCommunity
import nl.tudelft.ipv8.util.toHex
import nl.tudelft.trustchain.currencyii.TrustChainHelper
import nl.tudelft.trustchain.currencyii.coin.WalletManagerAndroid
import nl.tudelft.trustchain.currencyii.sharedWallet.SWJoinBlockTransactionData
import nl.tudelft.trustchain.currencyii.util.taproot.TaprootUtil
import org.bitcoinj.core.Coin
import org.bitcoinj.core.ECKey

class DAOCreateHelper {
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
     * If the transaction is valid, the result is broadcasted on trust chain.
     * **Throws** exceptions if something goes wrong with creating or broadcasting bitcoin transaction.
     */
    fun createBitcoinGenesisWallet(
        myPeer: Peer,
        entranceFee: Long,
        threshold: Int,
        context: Context
    ): SWJoinBlockTransactionData {
        val walletManager = WalletManagerAndroid.getInstance()
        val (_, serializedTransaction) = walletManager.safeCreationAndSendGenesisWallet(
            Coin.valueOf(entranceFee)
        )

        // Broadcast on trust chain if no errors are thrown in the previous step.
        return broadcastCreatedSharedWallet(
            myPeer,
            serializedTransaction,
            entranceFee,
            threshold,
            context
        )
    }

    /**
     * 1.2 Finishes the last step of creating a genesis shared bitcoin wallet.
     * Posts a self-signed trust chain block containing the shared wallet data.
     */
    fun broadcastCreatedSharedWallet(
        myPeer: Peer,
        transactionSerialized: String,
        entranceFee: Long,
        votingThreshold: Int,
        context: Context
    ): SWJoinBlockTransactionData {
        val walletManager = WalletManagerAndroid.getInstance()
        val bitcoinPublicKey = walletManager.networkPublicECKeyHex()
        val trustChainPk = myPeer.publicKey.keyToBin()
        val nonceKey = TaprootUtil.generate_schnorr_nonce(ECKey().privKeyBytes)
        val noncePoint = nonceKey.second.getEncoded(true).toHex()

        val blockData = SWJoinBlockTransactionData(
            entranceFee,
            transactionSerialized,
            votingThreshold,
            arrayListOf(trustChainPk.toHex()),
            arrayListOf(bitcoinPublicKey),
            arrayListOf(noncePoint)
        )

        walletManager.storeNonceKey(blockData.getData().SW_UNIQUE_ID, context, nonceKey.first.privKeyBytes.toHex())

        trustchain.createProposalBlock(blockData.getJsonString(), trustChainPk, blockData.blockType)
        return blockData
    }
}
