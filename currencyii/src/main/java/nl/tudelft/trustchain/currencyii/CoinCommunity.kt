package nl.tudelft.trustchain.currencyii

import android.util.Log
import nl.tudelft.ipv8.Community
import nl.tudelft.ipv8.android.IPv8Android
import nl.tudelft.trustchain.currencyii.coin.WalletManager
import nl.tudelft.trustchain.currencyii.coin.WalletManagerAndroid
import nl.tudelft.ipv8.attestation.trustchain.*
import nl.tudelft.ipv8.util.hexToBytes
import nl.tudelft.ipv8.util.toHex
import nl.tudelft.trustchain.currencyii.sharedWallet.*
import org.bitcoinj.core.Coin
import org.bitcoinj.core.ECKey
import org.bitcoinj.core.Transaction
import org.json.JSONException
import java.util.concurrent.TimeUnit

@Suppress("UNCHECKED_CAST")
class CoinCommunity : Community() {
    override val serviceId = "02313685c1912a141279f8248fc8db5899c5df5b"

    private val trustchain: TrustChainHelper by lazy {
        TrustChainHelper(getTrustChainCommunity())
    }

    private fun getTrustChainCommunity(): TrustChainCommunity {
        return IPv8Android.getInstance().getOverlay()
            ?: throw IllegalStateException("TrustChainCommunity is not configured")
    }

    /**
     * 1.1 Create a shared wallet block.
     * The bitcoin transaction may take some time to finish.
     * If the transaction is valid, the result is broadcasted on trust chain.
     * **Throws** exceptions if something goes wrong with creating or broadcasting bitcoin transaction.
     */
    public fun createBitcoinGenesisWallet(
        entranceFee: Long,
        threshold: Int,
        progressCallback: ((progress: Double) -> Unit)? = null,
        timeout: Long = DEFAULT_BITCOIN_MAX_TIMEOUT
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
        val serializedTransaction = getSerializedTransaction(transaction)

        // Broadcast on trust chain if no errors are thrown in the previous step.
        broadcastCreatedSharedWallet(
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

    /**
     * 2.1 Send a proposal on the trust chain to join a shared wallet and to collect signatures.
     * The proposal is a serialized bitcoin join transaction.
     * **NOTE** the latest walletBlockData should be given, otherwise the serialized transaction is invalid.
     * @param walletBlockData - the latest (that you know of) shared wallet block.
     */
    public fun proposeJoinWalletOnTrustChain(
        walletBlockData: TrustChainTransaction
    ): SWSignatureAskTransactionData {
        val blockData = SWJoinBlockTransactionData(walletBlockData).getData()

        val serializedTransaction =
            createBitcoinSharedWalletForJoining(blockData).serializedTransaction

        val oldTransactionSerialized = blockData.SW_TRANSACTION_SERIALIZED
        val total = blockData.SW_BITCOIN_PKS.size
        val requiredSignatures =
            SWUtil.percentageToIntThreshold(total, blockData.SW_VOTING_THRESHOLD)

        var askSignatureBlockData = SWSignatureAskTransactionData(
            blockData.SW_UNIQUE_ID,
            serializedTransaction,
            oldTransactionSerialized,
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
                oldTransactionSerialized,
                requiredSignatures,
                swParticipantPk
            )

            trustchain.createProposalBlock(
                askSignatureBlockData.getJsonString(),
                swParticipantPk.hexToBytes(),
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
    fun safeSendingJoinWalletTransaction(
        walletBlockData: TrustChainTransaction,
        blockData: SWSignatureAskBlockTD,
        signatures: List<String>,
        progressCallback: ((progress: Double) -> Unit)? = null,
        timeout: Long = DEFAULT_BITCOIN_MAX_TIMEOUT
    ) {
        val oldTransactionSerialized = blockData.SW_TRANSACTION_SERIALIZED_OLD
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
        val serializedTransaction = getSerializedTransaction(transaction)

        val oldWalletBlockData = SWJoinBlockTransactionData(walletBlockData)
        broadcastJoinedSharedWallet(oldWalletBlockData, serializedTransaction)
    }

    /**
     * 2.3 Broadcast the final shared wallet join block to the trust chain.
     */
    private fun broadcastJoinedSharedWallet(
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

    /**
     * Try to fetch the serialized transaction of a trust chain block.
     * @return serializedTransaction string if it exists.
     */
    private fun tryToFetchSerializedTransaction(block: TrustChainBlock): String? {
        return try {
            SWUtil.parseTransaction(block.transaction).get(SW_TRANSACTION_SERIALIZED).asString
        } catch (exception: JSONException) {
            null
        }
    }

    /**
     * 3.1 Send a proposal block on trustchain to ask for the signatures.
     * Assumed that people agreed to the transfer.
     */
    public fun askForTransferFundsSignatures(
        walletData: SWJoinBlockTD,
        receiverAddressSerialized: String,
        satoshiAmount: Long
    ): SWTransferFundsAskTransactionData {
        val oldTransactionSerialized = walletData.SW_TRANSACTION_SERIALIZED
        val total = walletData.SW_BITCOIN_PKS.size
        val requiredSignatures =
            SWUtil.percentageToIntThreshold(total, walletData.SW_VOTING_THRESHOLD)

        var askSignatureBlockData = SWTransferFundsAskTransactionData(
            walletData.SW_UNIQUE_ID,
            oldTransactionSerialized,
            requiredSignatures,
            satoshiAmount,
            walletData.SW_BITCOIN_PKS,
            receiverAddressSerialized,
            ""
        )

        for (swParticipantPk in walletData.SW_TRUSTCHAIN_PKS) {
            Log.i(
                "Coin",
                "Sending TRANSFER proposal (total: ${walletData.SW_TRUSTCHAIN_PKS.size}) to $swParticipantPk"
            )
            askSignatureBlockData = SWTransferFundsAskTransactionData(
                walletData.SW_UNIQUE_ID,
                oldTransactionSerialized,
                requiredSignatures,
                satoshiAmount,
                walletData.SW_BITCOIN_PKS,
                receiverAddressSerialized,
                swParticipantPk
            )

            trustchain.createProposalBlock(
                askSignatureBlockData.getJsonString(),
                swParticipantPk.hexToBytes(),
                askSignatureBlockData.blockType
            )
        }
        return askSignatureBlockData
    }

    /**
     * 3.2 Transfer funds from an existing shared wallet to a third-party. Broadcast bitcoin transaction.
     */
    public fun transferFunds(
        transferFundsData: SWTransferFundsAskTransactionData,
        walletData: SWJoinBlockTD,
        serializedSignatures: List<String>,
        receiverAddress: String,
        satoshiAmount: Long,
        progressCallback: ((progress: Double) -> Unit)? = null,
        timeout: Long = DEFAULT_BITCOIN_MAX_TIMEOUT
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
        val serializedTransaction = getSerializedTransaction(transaction)
        broadcastTransferFundSuccessful(
            walletData,
            transferFundsData,
            serializedTransaction
        )
    }

    /**
     * 3.3 Everything is done, publish the final serialized bitcoin transaction data on trustchain.
     */
    private fun broadcastTransferFundSuccessful(
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

    /**
     * Fetch blocks with type SHARED_WALLET_BLOCK.
     */
    private fun fetchSharedWalletBlocks(): List<TrustChainBlock> {
        return fetchSharedWalletBlocks(JOIN_BLOCK)
    }

    private fun fetchSharedWalletBlocks(blockType: String): List<TrustChainBlock> {
        var result = arrayListOf<TrustChainBlock>()
        result.addAll(getTrustChainCommunity().database.getBlocksWithType(blockType))

        return result
    }

    /**
     * Fetch the latest shared wallet block, based on a given block 'block'.
     * The unique shared wallet id is used to find the most recent block in
     * the 'sharedWalletBlocks' list.
     */
    private fun fetchLatestSharedWalletBlock(
        block: TrustChainBlock,
        fromBlocks: List<TrustChainBlock>
    ): TrustChainBlock? {
        // TODO: only fetch shared wallet blocks with [SW_TRANSACTION_SERIALIZED] in it
        val walletId = SWUtil.parseTransaction(block.transaction).get(SW_UNIQUE_ID).asString
        return fromBlocks
            .filter { SWUtil.parseTransaction(it.transaction).get(SW_UNIQUE_ID).asString == walletId }
            .maxBy { it.timestamp.time }
    }

    /**
     * Discover shared wallets that you can join, return the latest (known) blocks
     * Fetch the latest block associated with a shared wallet.
     * swBlockHash - the hash of one of the blocks associated with a shared wallet.
     */
    public fun fetchLatestSharedWalletTransactionBlock(swBlockHash: ByteArray): TrustChainBlock? {
        val swBlock = getTrustChainCommunity().database.getBlockWithHash(swBlockHash)
            ?: return null
        val transactionBlocks = fetchSharedWalletBlocks(JOIN_BLOCK)
        return fetchLatestSharedWalletBlock(swBlock, transactionBlocks)
    }

    /**
     * Discover shared wallets that you can join, return the latest (known) blocks.
     */
    public fun discoverSharedWallets(): List<TrustChainBlock> {
        val sharedWalletBlocks = fetchSharedWalletBlocks()
        // For every distinct unique shared wallet, find the latest block
        return sharedWalletBlocks
            .distinctBy { SWUtil.parseTransaction(it.transaction).get(SW_UNIQUE_ID).asString }
            .map { fetchLatestSharedWalletBlock(it, sharedWalletBlocks) ?: it }
    }

    /**
     * Fetch the shared wallet blocks that you are part of, based on your trustchain PK.
     */
    public fun fetchLatestJoinedSharedWalletBlocks(): List<TrustChainBlock> {
        return discoverSharedWallets().filter {
            val blockData = SWUtil.parseTransaction(it.transaction)

            val userTrustchainPks =
                SWUtil.parseJSONArray(blockData.get(SW_TRUSTCHAIN_PKS).asJsonArray)
            userTrustchainPks.contains(myPeer.publicKey.keyToBin().toHex())
        }
    }

    public fun fetchProposalSignatures(walletId: String, proposalId: String): List<String> {
        return getTrustChainCommunity().database.getBlocksWithType(SIGNATURE_AGREEMENT_BLOCK)
            .filter {
                val blockData = SWResponseSignatureTransactionData(it.transaction)
                blockData.matchesProposal(walletId, proposalId)
            }.map {
                val blockData = SWResponseSignatureTransactionData(it.transaction).getData()
                blockData.SW_SIGNATURE_SERIALIZED
            }
    }

    companion object {
        /**
         * Given a shared wallet proposal block, calculate the signature and send an agreement block.
         * Called by the listener of the [SIGNATURE_ASK_BLOCK] type. Respond with [SIGNATURE_AGREEMENT_BLOCK].
         */
        fun joinAskBlockReceived(block: TrustChainBlock, myPublicKey: ByteArray) {
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

            val oldTransactionSerialized = blockData.SW_TRANSACTION_SERIALIZED_OLD
            val newTransactionSerialized = blockData.SW_TRANSACTION_SERIALIZED
            val signature = walletManager.safeSigningJoinWalletTransaction(
                Transaction(walletManager.params, newTransactionSerialized.hexToBytes()),
                Transaction(walletManager.params, oldTransactionSerialized.hexToBytes()),
                walletManager.protocolECKey()
            )
            val signatureSerialized = signature.encodeToDER().toHex()
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
        }

        /**
         * Given a shared wallet transfer fund proposal block, calculate the signature and send an agreement block.
         */
        public fun transferFundsBlockReceived(block: TrustChainBlock, myPublicKey: ByteArray) {
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
                blockData.SW_TRANSACTION_SERIALIZED_OLD.hexToBytes()
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
        }

        /**
         * Helper method that serializes a bitcoin transaction to a string.
         */
        public fun getSerializedTransaction(transaction: Transaction): String {
            return transaction.bitcoinSerialize().toHex()
        }

        // Default maximum wait timeout for bitcoin transaction broadcasts in seconds
        private const val DEFAULT_BITCOIN_MAX_TIMEOUT: Long = 60 * 5

        // Block type for join DAO blocks
        public const val JOIN_BLOCK = "DAO_JOIN"

        // Block type for transfer funds (from a DAO)
        public const val TRANSFER_FINAL_BLOCK = "DAO_TRANSFER_FINAL"

        // Block type for basic signature requests
        public const val SIGNATURE_ASK_BLOCK = "DAO_ASK_SIGNATURE"

        // Block type for transfer funds signature requests
        public const val TRANSFER_FUNDS_ASK_BLOCK = "DAO_TRANSFER_ASK_SIGNATURE"

        // Block type for responding to a signature request with a (should be valid) signature
        public const val SIGNATURE_AGREEMENT_BLOCK = "DAO_SIGNATURE_AGREEMENT"

        // Values below are present in SW_TRANSACTION_BLOCK_KEYS block types
        public const val SW_UNIQUE_ID = "SW_UNIQUE_ID"
        public const val SW_TRANSACTION_SERIALIZED = "SW_TRANSACTION_SERIALIZED"
        public const val SW_TRUSTCHAIN_PKS = "SW_TRUSTCHAIN_PKS"
    }
}
