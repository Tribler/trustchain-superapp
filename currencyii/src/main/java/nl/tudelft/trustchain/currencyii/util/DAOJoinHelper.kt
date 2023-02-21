package nl.tudelft.trustchain.currencyii.util

import android.content.Context
import android.util.Log
import nl.tudelft.ipv8.Peer
import nl.tudelft.ipv8.android.IPv8Android
import nl.tudelft.ipv8.attestation.trustchain.TrustChainBlock
import nl.tudelft.ipv8.attestation.trustchain.TrustChainCommunity
import nl.tudelft.ipv8.attestation.trustchain.TrustChainTransaction
import nl.tudelft.ipv8.util.hexToBytes
import nl.tudelft.ipv8.util.toHex
import nl.tudelft.trustchain.currencyii.CoinCommunity.Companion.SIGNATURE_AGREEMENT_BLOCK
import nl.tudelft.trustchain.currencyii.CoinCommunity.Companion.SIGNATURE_ASK_BLOCK
import nl.tudelft.trustchain.currencyii.TrustChainHelper
import nl.tudelft.trustchain.currencyii.coin.WalletManagerAndroid
import nl.tudelft.trustchain.currencyii.sharedWallet.*
import nl.tudelft.trustchain.currencyii.util.taproot.CTransaction
import nl.tudelft.trustchain.currencyii.util.taproot.MuSig
import org.bitcoinj.core.Coin
import org.bitcoinj.core.ECKey
import java.math.BigInteger

class DAOJoinHelper {
    private fun getTrustChainCommunity(): TrustChainCommunity {
        return IPv8Android.getInstance().getOverlay()
            ?: throw IllegalStateException("TrustChainCommunity is not configured")
    }

    private val trustchain: TrustChainHelper by lazy {
        TrustChainHelper(getTrustChainCommunity())
    }

    /**
     * 2.1 Send a proposal on the trust chain to join a shared wallet and to collect signatures.
     * The proposal is a serialized bitcoin join transaction.
     * **NOTE** the latest walletBlockData should be given, otherwise the serialized transaction is invalid.
     * @param mostRecentWalletBlock - the latest (that you know of) shared wallet block.
     */
    fun proposeJoinWallet(
        myPeer: Peer,
        mostRecentWalletBlock: TrustChainBlock
    ): SWSignatureAskTransactionData {
        val mostRecentBlockHash = mostRecentWalletBlock.calculateHash().toHex()
        val blockData = SWJoinBlockTransactionData(mostRecentWalletBlock.transaction).getData()

        val serializedTransaction =
            createBitcoinSharedWalletForJoining(blockData)

        val total = blockData.SW_BITCOIN_PKS.size
        val requiredSignatures =
            SWUtil.percentageToIntThreshold(total, blockData.SW_VOTING_THRESHOLD)

        val proposalIDSignature = SWUtil.randomUUID()

        var askSignatureBlockData = SWSignatureAskTransactionData(
            blockData.SW_UNIQUE_ID,
            serializedTransaction,
            mostRecentBlockHash,
            requiredSignatures,
            "",
            proposalIDSignature
        )

        for (swParticipantPk in blockData.SW_TRUSTCHAIN_PKS) {
            Log.i(
                "Coin",
                "Sending JOIN proposal (total: ${blockData.SW_TRUSTCHAIN_PKS.size}) to $swParticipantPk"
            )

            askSignatureBlockData = SWSignatureAskTransactionData(
                blockData.SW_UNIQUE_ID,
                serializedTransaction,
                mostRecentBlockHash,
                requiredSignatures,
                swParticipantPk,
                proposalIDSignature
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
     * 2.1.1 This functions handles the process of creating a bitcoin DAO join transaction.
     * Create a bitcoin transaction that creates a new shared wallet. This takes some time to complete.
     *
     * NOTE:
     *  - the latest walletBlockData should be given, otherwise the serialized transaction is invalid.
     *  - It takes some time before the shared wallet is accepted on the bitcoin blockchain.
     * @param sharedWalletData data of the shared wallet that you want to join.
     */
    private fun createBitcoinSharedWalletForJoining(sharedWalletData: SWJoinBlockTD): String {
        val walletManager = WalletManagerAndroid.getInstance()

        val oldTransaction = sharedWalletData.SW_TRANSACTION_SERIALIZED
        val bitcoinPublicKeys = ArrayList<String>()
        bitcoinPublicKeys.addAll(sharedWalletData.SW_BITCOIN_PKS)
        bitcoinPublicKeys.add(walletManager.networkPublicECKeyHex())

        return walletManager.safeCreationJoinWalletTransaction(
            bitcoinPublicKeys,
            Coin.valueOf(sharedWalletData.SW_ENTRANCE_FEE),
            oldTransaction
        )
    }

    /**
     * 2.2 Commit the join wallet transaction on the bitcoin blockchain and broadcast the result on trust chain.
     *
     * Note:
     * There should be enough sufficient signatures, based on the multisig wallet data.
     * @throws - exceptions if something goes wrong with creating or broadcasting bitcoin transaction.
     * @param myPeer - Peer, the user that wants to join the wallet
     * @param walletBlockData - TrustChainTransaction, describes the wallet that is joined
     * @param blockData - SWSignatureAskBlockTD, the block where the other users are voting on
     * @param responses - the positive responses for your request to join the wallet
     */
    fun joinBitcoinWallet(
        myPeer: Peer,
        walletBlockData: TrustChainTransaction,
        blockData: SWSignatureAskBlockTD,
        responses: List<SWResponseSignatureBlockTD>,
        context: Context
    ) {
        val oldWalletBlockData = SWJoinBlockTransactionData(walletBlockData)
        val newTransactionSerialized = blockData.SW_TRANSACTION_SERIALIZED

        val walletManager = WalletManagerAndroid.getInstance()

        val signaturesOfOldOwners = responses.map {
            BigInteger(1, it.SW_SIGNATURE_SERIALIZED.hexToBytes())
        }

        val newNonces: ArrayList<String> = ArrayList(responses.map { it.SW_NONCE })

        val noncePoints = oldWalletBlockData.getData().SW_NONCE_PKS.map {
            ECKey.fromPublicOnly(it.hexToBytes())
        }

        val (aggregateNoncePoint, _) = MuSig.aggregate_schnorr_nonces(noncePoints)

        val newTransactionProposal = newTransactionSerialized.hexToBytes()
        val (status, serializedTransaction) = walletManager.safeSendingJoinWalletTransaction(
            signaturesOfOldOwners,
            aggregateNoncePoint,
            CTransaction().deserialize(newTransactionProposal)
        )

        if (status) {
            Log.i("Coin", "Successfully submitted taproot transaction to server")
        } else {
            Log.e("Coin", "Taproot transaction submission to server failed")
        }

        oldWalletBlockData.getData().SW_NONCE_PKS = newNonces

        broadcastJoinedSharedWallet(myPeer, oldWalletBlockData, serializedTransaction, context)
    }

    /**
     * 2.3 Broadcast the final shared wallet join block to the trust chain.
     */
    private fun broadcastJoinedSharedWallet(
        myPeer: Peer,
        oldBlockData: SWJoinBlockTransactionData,
        serializedTransaction: String,
        context: Context
    ) {
        val newData = SWJoinBlockTransactionData(oldBlockData.jsonData)
        val walletManager = WalletManagerAndroid.getInstance()

        newData.addBitcoinPk(walletManager.networkPublicECKeyHex())
        newData.addTrustChainPk(myPeer.publicKey.keyToBin().toHex())
        newData.setTransactionSerialized(serializedTransaction)
        newData.addNoncePk(
            walletManager.addNewNonceKey(
                oldBlockData.getData().SW_UNIQUE_ID,
                context
            ).second.getEncoded(true).toHex()
        )

        trustchain.createProposalBlock(
            newData.getJsonString(),
            myPeer.publicKey.keyToBin(),
            newData.blockType
        )
    }

    companion object {
        /**
         * Given a shared wallet proposal block, calculate the signature and send an agreement block.
         * Called by the listener of the [SIGNATURE_ASK_BLOCK] type. Respond with [SIGNATURE_AGREEMENT_BLOCK].
         */
        fun joinAskBlockReceived(
            oldTransactionSerialized: String,
            block: TrustChainBlock,
            joinBlock: SWJoinBlockTD,
            myPublicKey: ByteArray,
            votedInFavor: Boolean,
            context: Context
        ) {
            val trustchain = TrustChainHelper(IPv8Android.getInstance().getOverlay() ?: return)
            val blockData = SWSignatureAskTransactionData(block.transaction).getData()

            Log.i(
                "Coin",
                "Signature request for joining: ${blockData.SW_RECEIVER_PK}, me: ${myPublicKey.toHex()}"
            )

            if (blockData.SW_RECEIVER_PK != myPublicKey.toHex()) {
                Log.i(
                    "Coin",
                    "${blockData.SW_RECEIVER_PK} != ${myPublicKey.toHex()}"
                )
                return
            }
            Log.i("Coin", "Signing join block transaction: $blockData")

            val walletManager = WalletManagerAndroid.getInstance()

            val newTransactionSerialized = blockData.SW_TRANSACTION_SERIALIZED
            val signature = walletManager.safeSigningJoinWalletTransaction(
                CTransaction().deserialize(oldTransactionSerialized.hexToBytes()),
                CTransaction().deserialize(newTransactionSerialized.hexToBytes()),
                joinBlock.SW_BITCOIN_PKS.map { ECKey.fromPublicOnly(it.hexToBytes()) },
                joinBlock.SW_NONCE_PKS.map { ECKey.fromPublicOnly(it.hexToBytes()) },
                walletManager.protocolECKey(),
                joinBlock.SW_UNIQUE_ID,
                context
            )

            val nonce = walletManager.addNewNonceKey(joinBlock.SW_UNIQUE_ID, context)

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
