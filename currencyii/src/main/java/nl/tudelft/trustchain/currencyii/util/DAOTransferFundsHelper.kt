package nl.tudelft.trustchain.currencyii.util

import android.app.Activity
import android.content.Context
import android.util.Log
import android.widget.Toast
import nl.tudelft.ipv8.Peer
import nl.tudelft.ipv8.android.IPv8Android
import nl.tudelft.ipv8.attestation.trustchain.TrustChainBlock
import nl.tudelft.ipv8.attestation.trustchain.TrustChainCommunity
import nl.tudelft.ipv8.attestation.trustchain.TrustChainTransaction
import nl.tudelft.ipv8.util.hexToBytes
import nl.tudelft.ipv8.util.toHex
import nl.tudelft.trustchain.currencyii.TrustChainHelper
import nl.tudelft.trustchain.currencyii.coin.WalletManagerAndroid
import nl.tudelft.trustchain.currencyii.sharedWallet.*
import nl.tudelft.trustchain.currencyii.util.taproot.MuSig
import org.bitcoinj.core.Address
import org.bitcoinj.core.ECKey
import java.math.BigInteger

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
     * @param myPeer - Peer, the user that wants to join the wallet
     * @param mostRecentWalletBlock - TrustChainBlock, describes the wallet where the transfer is from
     * @param receiverAddressSerialized - String, the address where the transaction needs to go
     * @param satoshiAmount - Long, the amount that needs to be transferred
     * @return the proposal block
     */
    fun proposeTransferFunds(
        myPeer: Peer,
        mostRecentWalletBlock: TrustChainBlock,
        receiverAddressSerialized: String,
        satoshiAmount: Long
    ): SWTransferFundsAskTransactionData {
        val mostRecentBlockHash = mostRecentWalletBlock.calculateHash().toHex()
        val blockData = SWJoinBlockTransactionData(mostRecentWalletBlock.transaction).getData()

        val total = blockData.SW_BITCOIN_PKS.size
        val requiredSignatures =
            SWUtil.percentageToIntThreshold(total, blockData.SW_VOTING_THRESHOLD)

        val proposalID = SWUtil.randomUUID()

        var askSignatureBlockData = SWTransferFundsAskTransactionData(
            blockData.SW_UNIQUE_ID,
            mostRecentBlockHash,
            requiredSignatures,
            satoshiAmount,
            blockData.SW_BITCOIN_PKS,
            receiverAddressSerialized,
            "",
            proposalID,
            blockData.SW_TRANSACTION_SERIALIZED
        )

        for (swParticipantPk in blockData.SW_TRUSTCHAIN_PKS) {
            Log.i(
                "Coin",
                "Sending TRANSFER proposal (total: ${blockData.SW_TRUSTCHAIN_PKS.size}) to $swParticipantPk"
            )
            askSignatureBlockData = SWTransferFundsAskTransactionData(
                blockData.SW_UNIQUE_ID,
                mostRecentBlockHash,
                requiredSignatures,
                satoshiAmount,
                blockData.SW_BITCOIN_PKS,
                receiverAddressSerialized,
                swParticipantPk,
                proposalID,
                blockData.SW_TRANSACTION_SERIALIZED
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
        walletData: SWJoinBlockTD,
        walletBlockData: TrustChainTransaction,
        blockData: SWTransferFundsAskBlockTD,
        responses: List<SWResponseSignatureBlockTD>,
        receiverAddress: String,
        paymentAmount: Long,
        context: Context,
        activity: Activity
    ) {
        val oldWalletBlockData = SWTransferDoneTransactionData(walletBlockData)
        val oldTransactionSerialized = blockData.SW_TRANSACTION_SERIALIZED

        val walletManager = WalletManagerAndroid.getInstance()

        val signaturesOfOldOwners = responses.map {
            BigInteger(1, it.SW_SIGNATURE_SERIALIZED.hexToBytes())
        }

        val noncePoints = oldWalletBlockData.getData().SW_NONCE_PKS.map {
            ECKey.fromPublicOnly(it.hexToBytes())
        }

        val newNonces: ArrayList<String> = ArrayList(responses.map { it.SW_NONCE })

        val (aggregateNoncePoint, _) = MuSig.aggregate_schnorr_nonces(noncePoints)

        val (status, serializedTransaction) = walletManager.safeSendingTransactionFromMultiSig(
            oldWalletBlockData.getData().SW_BITCOIN_PKS.map { ECKey.fromPublicOnly(it.hexToBytes()) },
            signaturesOfOldOwners,
            aggregateNoncePoint,
            oldTransactionSerialized,
            Address.fromString(walletManager.params, receiverAddress),
            paymentAmount
        )

        if (status) {
            Log.d("MVDAO", "successfully submitted taproot transaction to server")
            activity.runOnUiThread {
                Toast.makeText(context, "Successfully submitted the transaction", Toast.LENGTH_SHORT).show()
            }
        } else {
            Log.d("MVDAO", "taproot transaction submission to server failed")
            activity.runOnUiThread {
                Toast.makeText(context, "Failed to submit the transaction to the server", Toast.LENGTH_SHORT).show()
            }
        }

        oldWalletBlockData.getData().SW_NONCE_PKS = newNonces

        broadcastTransferFundSuccessful(myPeer, walletData, oldWalletBlockData, serializedTransaction)
    }

    /**
     * 3.3 Everything is done, publish the final serialized bitcoin transaction data on trustchain.
     */
    private fun broadcastTransferFundSuccessful(
        myPeer: Peer,
        walletData: SWJoinBlockTD,
        oldBlockData: SWTransferDoneTransactionData,
        serializedTransaction: String
    ) {
        val newData = SWTransferDoneTransactionData(oldBlockData.jsonData)
        newData.setTransactionSerialized(serializedTransaction)

        val refreshDaoBlock = SWJoinBlockTransactionData(
            walletData.SW_ENTRANCE_FEE,
            serializedTransaction,
            walletData.SW_VOTING_THRESHOLD,
            walletData.SW_TRUSTCHAIN_PKS,
            walletData.SW_BITCOIN_PKS,
            walletData.SW_NONCE_PKS,
            walletData.SW_UNIQUE_ID
        )

        trustchain.createProposalBlock(
            newData.getJsonString(),
            myPeer.publicKey.keyToBin(),
            newData.blockType
        )

        trustchain.createProposalBlock(
            refreshDaoBlock.getJsonString(),
            myPeer.publicKey.keyToBin(),
            refreshDaoBlock.blockType
        )
    }

    companion object {
        /**
         * Given a shared wallet transfer fund proposal block, calculate the signature and send an agreement block.
         */
        fun transferFundsBlockReceived(
            oldTransactionSerialized: String,
            block: TrustChainBlock,
            transferBlock: SWTransferDoneBlockTD,
            myPublicKey: ByteArray,
            votedInFavor: Boolean,
            context: Context
        ) {
            val trustchain = TrustChainHelper(IPv8Android.getInstance().getOverlay() ?: return)
            val blockData = SWTransferFundsAskTransactionData(block.transaction).getData()

            Log.i("Coin", "Signature request for transfer funds: ${blockData.SW_RECEIVER_PK}, me: ${myPublicKey.toHex()}")

            if (blockData.SW_RECEIVER_PK != myPublicKey.toHex()) {
                return
            }

            Log.i("Coin", "Signing transfer funds transaction: $blockData")

            val walletManager = WalletManagerAndroid.getInstance()

            val signature = walletManager.safeSigningTransactionFromMultiSig(
                oldTransactionSerialized,
                transferBlock.SW_BITCOIN_PKS.map { ECKey.fromPublicOnly(it.hexToBytes()) },
                transferBlock.SW_NONCE_PKS.map { ECKey.fromPublicOnly(it.hexToBytes()) },
                walletManager.protocolECKey(),
                Address.fromString(walletManager.params, blockData.SW_TRANSFER_FUNDS_TARGET_SERIALIZED),
                blockData.SW_TRANSFER_FUNDS_AMOUNT,
                blockData.SW_UNIQUE_ID,
                context
            )

            val nonce = walletManager.addNewNonceKey(transferBlock.SW_UNIQUE_ID, context)

            val signatureSerialized = signature.toByteArray().toHex()

            if (votedInFavor) {
                val agreementData = SWResponseSignatureTransactionData(
                    blockData.SW_UNIQUE_ID,
                    blockData.SW_UNIQUE_PROPOSAL_ID,
                    signatureSerialized,
                    walletManager.protocolECKey().publicKeyAsHex,
                    walletManager.nonceECPointHex(nonce)
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
                    signatureSerialized,
                    walletManager.protocolECKey().publicKeyAsHex,
                    walletManager.nonceECPointHex(nonce)
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
