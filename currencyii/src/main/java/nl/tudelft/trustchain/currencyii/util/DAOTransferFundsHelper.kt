package nl.tudelft.trustchain.currencyii.util

import android.util.Log
import nl.tudelft.ipv8.Peer
import nl.tudelft.ipv8.android.IPv8Android
import nl.tudelft.ipv8.attestation.trustchain.TrustChainBlock
import nl.tudelft.ipv8.attestation.trustchain.TrustChainCommunity
import nl.tudelft.ipv8.util.hexToBytes
import nl.tudelft.ipv8.util.toHex
import nl.tudelft.trustchain.currencyii.CoinCommunity
import nl.tudelft.trustchain.currencyii.TrustChainHelper
import nl.tudelft.trustchain.currencyii.coin.WalletManagerAndroid
import nl.tudelft.trustchain.currencyii.sharedWallet.*
import org.bitcoinj.core.Coin
import org.bitcoinj.core.ECKey
import org.bitcoinj.core.Transaction
import java.util.concurrent.TimeUnit

class DAOTransferFundsHelper {
    private fun getTrustChainCommunity(): TrustChainCommunity {
        return IPv8Android.getInstance().getOverlay()
            ?: throw IllegalStateException("TrustChainCommunity is not configured")
    }

    private val trustchain: TrustChainHelper by lazy {
        TrustChainHelper(getTrustChainCommunity())
    }

    /**
     * 3.1 Send a proposal block on trustchain to ask for the signatures.
     * Assumed that people agreed to the transfer.
     */
    fun proposeTransferFunds(
        myPeer: Peer,
        mostRecentWallet: TrustChainBlock,
        receiverAddressSerialized: String,
        satoshiAmount: Long
    ): SWTransferFundsAskTransactionData {
        val walletData = SWJoinBlockTransactionData(mostRecentWallet.transaction).getData()
        val walletHash = mostRecentWallet.calculateHash().toHex()

        val total = walletData.SW_BITCOIN_PKS.size
        val requiredSignatures =
            SWUtil.percentageToIntThreshold(total, walletData.SW_VOTING_THRESHOLD)

        val proposalID = SWUtil.randomUUID()

        var askSignatureBlockData = SWTransferFundsAskTransactionData(
            walletData.SW_UNIQUE_ID,
            walletHash,
            requiredSignatures,
            satoshiAmount,
            walletData.SW_BITCOIN_PKS,
            receiverAddressSerialized,
            "",
            proposalID,
            walletData.SW_TRANSACTION_SERIALIZED
        )

        for (swParticipantPk in walletData.SW_TRUSTCHAIN_PKS) {
            Log.i(
                "Coin",
                "Sending TRANSFER proposal (total: ${walletData.SW_TRUSTCHAIN_PKS.size}) to $swParticipantPk"
            )
            askSignatureBlockData = SWTransferFundsAskTransactionData(
                walletData.SW_UNIQUE_ID,
                walletHash,
                requiredSignatures,
                satoshiAmount,
                walletData.SW_BITCOIN_PKS,
                receiverAddressSerialized,
                swParticipantPk,
                proposalID,
                walletData.SW_TRANSACTION_SERIALIZED
            )

            trustchain.createProposalBlock(
                askSignatureBlockData.getJsonString(),
                myPeer.publicKey.keyToBin(),
                askSignatureBlockData.blockType
            )
        }
        return askSignatureBlockData
    }

    /**
     * 3.2 Transfer funds from an existing shared wallet to a third-party. Broadcast bitcoin transaction.
     */
    fun transferFunds(
        myPeer: Peer,
        transferFundsData: SWTransferFundsAskTransactionData,
        walletData: SWJoinBlockTD,
        serializedSignatures: List<String>,
        receiverAddress: String,
        satoshiAmount: Long,
        progressCallback: ((progress: Double) -> Unit)? = null,
        timeout: Long = CoinCommunity.DEFAULT_BITCOIN_MAX_TIMEOUT
    ) {
        val walletManager = WalletManagerAndroid.getInstance()
        val bitcoinTransaction =
            Transaction(walletManager.params, walletData.SW_TRANSACTION_SERIALIZED.hexToBytes())

        val signatures = serializedSignatures.map {
            ECKey.ECDSASignature.decodeFromDER(it.hexToBytes())
        }

        val transactionBroadcast = walletManager.safeSendingTransactionFromMultiSig(
            bitcoinTransaction,
            signatures,
            org.bitcoinj.core.Address.fromString(walletManager.params, receiverAddress),
            Coin.valueOf(satoshiAmount)
        )

        if (progressCallback != null) {
            transactionBroadcast.setProgressCallback { progress ->
                progressCallback(progress)
            }
        }

        val transaction = transactionBroadcast.broadcast().get(timeout, TimeUnit.SECONDS)
        val serializedTransaction = CoinCommunity.getSerializedTransaction(transaction)

        // Publish the result on trust chain, if no errors were thrown during transaction initialization.
        broadcastTransferFundSuccessful(
            myPeer,
            walletData,
            transferFundsData,
            serializedTransaction
        )
    }

    /**
     * 3.3 Everything is done, publish the final serialized bitcoin transaction data on trustchain.
     */
    private fun broadcastTransferFundSuccessful(
        myPeer: Peer,
        walletData: SWJoinBlockTD,
        initialData: SWTransferFundsAskTransactionData,
        transactionSerialized: String
    ) {
        val initialBlockData = initialData.getData()

        val transactionData = SWTransferDoneTransactionData(
            initialBlockData.SW_UNIQUE_ID,
            transactionSerialized,
            initialBlockData.SW_TRANSFER_FUNDS_AMOUNT,
            initialBlockData.SW_BITCOIN_PKS,
            initialBlockData.SW_TRANSFER_FUNDS_TARGET_SERIALIZED
        )

        val shareData = SWJoinBlockTransactionData(
            walletData.SW_ENTRANCE_FEE,
            transactionSerialized,
            walletData.SW_VOTING_THRESHOLD,
            walletData.SW_TRUSTCHAIN_PKS,
            walletData.SW_BITCOIN_PKS,
            walletData.SW_UNIQUE_ID
        )

        // Broadcast transfer funds results
        trustchain.createProposalBlock(
            transactionData.getJsonString(),
            myPeer.publicKey.keyToBin(),
            transactionData.blockType
        )

        // Broadcast new wallet data (containing newest serialized transaction
        trustchain.createProposalBlock(
            shareData.getJsonString(),
            myPeer.publicKey.keyToBin(),
            shareData.blockType
        )
    }

    companion object {
        /**
         * Given a shared wallet transfer fund proposal block, calculate the signature and send an agreement block.
         */
        fun transferFundsBlockReceived(
            oldTransactionSerialized: String,
            block: TrustChainBlock,
            myPublicKey: ByteArray,
            votedInFavor: Boolean
        ) {
            val trustchain = TrustChainHelper(IPv8Android.getInstance().getOverlay() ?: return)
            val walletManager = WalletManagerAndroid.getInstance()
            val blockData = SWTransferFundsAskTransactionData(block.transaction).getData()

            Log.i(
                "Coin",
                "Signature request for transfer funds: ${blockData.SW_RECEIVER_PK}, me: ${myPublicKey.toHex()}"
            )

            if (blockData.SW_RECEIVER_PK != myPublicKey.toHex()) {
                return
            }

            Log.i("Coin", "Signing transfer funds transaction: $blockData")
            val satoshiAmount = Coin.valueOf(blockData.SW_TRANSFER_FUNDS_AMOUNT)
            val previousTransaction = Transaction(
                walletManager.params,
                oldTransactionSerialized.hexToBytes()
            )
            val receiverAddress = org.bitcoinj.core.Address.fromString(
                walletManager.params,
                blockData.SW_TRANSFER_FUNDS_TARGET_SERIALIZED
            )

            val signature = walletManager.safeSigningTransactionFromMultiSig(
                previousTransaction,
                walletManager.protocolECKey(),
                receiverAddress,
                satoshiAmount
            )

            val signatureSerialized = signature.encodeToDER().toHex()
            if (votedInFavor) {
                val agreementData = SWResponseSignatureTransactionData(
                    blockData.SW_UNIQUE_ID,
                    blockData.SW_UNIQUE_PROPOSAL_ID,
                    signatureSerialized
                )

                trustchain.createProposalBlock(
                    agreementData.getTransactionData(),
                    myPublicKey,
                    agreementData.blockType
                )
            } else {
                val negativeResponseData = SWResponseNegativeSignatureTransactionData(
                    blockData.SW_UNIQUE_ID,
                    blockData.SW_UNIQUE_PROPOSAL_ID,
                    signatureSerialized
                )

                trustchain.createProposalBlock(
                    negativeResponseData.getTransactionData(),
                    myPublicKey,
                    negativeResponseData.blockType
                )
            }
        }
    }
}
