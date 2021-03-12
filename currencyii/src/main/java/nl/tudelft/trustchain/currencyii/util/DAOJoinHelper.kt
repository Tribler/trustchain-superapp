package nl.tudelft.trustchain.currencyii.util

import android.util.Log
import nl.tudelft.ipv8.Peer
import nl.tudelft.ipv8.android.IPv8Android
import nl.tudelft.ipv8.attestation.trustchain.TrustChainBlock
import nl.tudelft.ipv8.attestation.trustchain.TrustChainCommunity
import nl.tudelft.ipv8.attestation.trustchain.TrustChainTransaction
import nl.tudelft.ipv8.util.hexToBytes
import nl.tudelft.ipv8.util.toHex
import nl.tudelft.trustchain.currencyii.CoinCommunity
import nl.tudelft.trustchain.currencyii.CoinCommunity.Companion.SIGNATURE_AGREEMENT_BLOCK
import nl.tudelft.trustchain.currencyii.CoinCommunity.Companion.SIGNATURE_ASK_BLOCK
import nl.tudelft.trustchain.currencyii.TrustChainHelper
import nl.tudelft.trustchain.currencyii.coin.WalletManager
import nl.tudelft.trustchain.currencyii.coin.WalletManagerAndroid
import nl.tudelft.trustchain.currencyii.sharedWallet.*
import org.bitcoinj.core.Coin
import org.bitcoinj.core.ECKey
import org.bitcoinj.core.Transaction
import java.util.concurrent.TimeUnit

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
    public fun proposeJoinWallet(
        myPeer: Peer,
        mostRecentWalletBlock: TrustChainBlock
    ): SWSignatureAskTransactionData {
        val mostRecentBlockHash = mostRecentWalletBlock.calculateHash().toHex()
        val blockData = SWJoinBlockTransactionData(mostRecentWalletBlock.transaction).getData()

        val serializedTransaction =
            createBitcoinSharedWalletForJoining(blockData).serializedTransaction

        val total = blockData.SW_BITCOIN_PKS.size
        val requiredSignatures =
            SWUtil.percentageToIntThreshold(total, blockData.SW_VOTING_THRESHOLD)

        var askSignatureBlockData = SWSignatureAskTransactionData(
            blockData.SW_UNIQUE_ID,
            serializedTransaction,
            mostRecentBlockHash,
            requiredSignatures,
            ""
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
                swParticipantPk
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
    private fun createBitcoinSharedWalletForJoining(sharedWalletData: SWJoinBlockTD): WalletManager.TransactionPackage {
        val walletManager = WalletManagerAndroid.getInstance()

        val oldTransaction = sharedWalletData.SW_TRANSACTION_SERIALIZED
        val bitcoinPublicKeys = ArrayList<String>()
        bitcoinPublicKeys.addAll(sharedWalletData.SW_BITCOIN_PKS)
        bitcoinPublicKeys.add(walletManager.networkPublicECKeyHex())
        val newThreshold =
            SWUtil.percentageToIntThreshold(
                bitcoinPublicKeys.size,
                sharedWalletData.SW_VOTING_THRESHOLD
            )

        return walletManager.safeCreationJoinWalletTransaction(
            bitcoinPublicKeys,
            Coin.valueOf(sharedWalletData.SW_ENTRANCE_FEE),
            Transaction(walletManager.params, oldTransaction.hexToBytes()),
            newThreshold
        )
    }

    /**
     * 2.2 Commit the join wallet transaction on the bitcoin blockchain and broadcast the result on trust chain.
     *
     * Note:
     * There should be enough sufficient signatures, based on the multisig wallet data.
     * **Throws** exceptions if something goes wrong with creating or broadcasting bitcoin transaction.
     */
    fun joinBitcoinWallet(
        myPeer: Peer,
        walletBlockData: TrustChainTransaction,
        blockData: SWSignatureAskBlockTD,
        signatures: List<String>,
        progressCallback: ((progress: Double) -> Unit)? = null,
        timeout: Long = CoinCommunity.DEFAULT_BITCOIN_MAX_TIMEOUT
    ) {
        val oldWalletBlockData = SWJoinBlockTransactionData(walletBlockData)

        val oldTransactionSerialized = oldWalletBlockData.getData().SW_TRANSACTION_SERIALIZED
        val newTransactionSerialized = blockData.SW_TRANSACTION_SERIALIZED

        val walletManager = WalletManagerAndroid.getInstance()

        val signaturesOfOldOwners = signatures.map {
            ECKey.ECDSASignature.decodeFromDER(it.hexToBytes())
        }
        val newTransactionProposal =
            Transaction(walletManager.params, newTransactionSerialized.hexToBytes())
        val oldTransaction =
            Transaction(walletManager.params, oldTransactionSerialized.hexToBytes())
        val transactionBroadcast = walletManager.safeSendingJoinWalletTransaction(
            signaturesOfOldOwners,
            newTransactionProposal,
            oldTransaction
        )

        if (progressCallback != null) {
            transactionBroadcast.setProgressCallback { progress ->
                progressCallback(progress)
            }
        }

        val transaction = transactionBroadcast.broadcast().get(timeout, TimeUnit.SECONDS)
        val serializedTransaction = CoinCommunity.getSerializedTransaction(transaction)

        broadcastJoinedSharedWallet(myPeer, oldWalletBlockData, serializedTransaction)
    }

    /**
     * 2.3 Broadcast the final shared wallet join block to the trust chain.
     */
    private fun broadcastJoinedSharedWallet(
        myPeer: Peer,
        oldBlockData: SWJoinBlockTransactionData,
        serializedTransaction: String
    ) {
        val newData = SWJoinBlockTransactionData(oldBlockData.jsonData)

        newData.addBitcoinPk(WalletManagerAndroid.getInstance().networkPublicECKeyHex())
        newData.addTrustChainPk(myPeer.publicKey.keyToBin().toHex())
        newData.setTransactionSerialized(serializedTransaction)

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
            myPublicKey: ByteArray,
            votedInFavor: Boolean
        ) {
            val trustchain = TrustChainHelper(IPv8Android.getInstance().getOverlay() ?: return)

            val blockData = SWSignatureAskTransactionData(block.transaction).getData()

            Log.i(
                "Coin",
                "Signature request for joining: ${blockData.SW_RECEIVER_PK}, me: ${myPublicKey.toHex()}"
            )

            if (blockData.SW_RECEIVER_PK != myPublicKey.toHex()) {
                return
            }
            Log.i("Coin", "Signing join block transaction: $blockData")

            val walletManager = WalletManagerAndroid.getInstance()

            val newTransactionSerialized = blockData.SW_TRANSACTION_SERIALIZED
            val signature = walletManager.safeSigningJoinWalletTransaction(
                Transaction(walletManager.params, newTransactionSerialized.hexToBytes()),
                Transaction(walletManager.params, oldTransactionSerialized.hexToBytes()),
                walletManager.protocolECKey()
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
