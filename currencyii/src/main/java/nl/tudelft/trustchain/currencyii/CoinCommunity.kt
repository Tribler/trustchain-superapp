package nl.tudelft.trustchain.currencyii

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

@Suppress("UNCHECKED_CAST")
class CoinCommunity : Community() {
    override val serviceId = "0000bitcoin0000community0000"

    private val trustchain: TrustChainHelper by lazy {
        TrustChainHelper(getTrustChainCommunity())
    }

    private fun getTrustChainCommunity(): TrustChainCommunity {
        return IPv8Android.getInstance().getOverlay()
            ?: throw IllegalStateException("TrustChainCommunity is not configured")
    }

    public fun fetchBitcoinTransactionStatus(transactionId: String): Boolean {
        return fetchBitcoinTransaction(transactionId) != null
    }

    /**
     * Get the serialized transaction of a bitcoin transaction id. Null if it does not exist (yet).
     */
    public fun fetchBitcoinTransaction(transactionId: String): String? {
        val walletManager = WalletManagerAndroid.getInstance()
        return walletManager.attemptToGetTransactionAndSerialize(transactionId)
    }

    /**
     * 1.1 Create a shared wallet block.
     * The transaction may take some time to finish. Use `fetchBitcoinTransactionStatus()` to get the status.
     * entranceFee - the fee that has to be paid for new participants
     */
    public fun createGenesisSharedWallet(entranceFee: Long): String {
        val walletManager = WalletManagerAndroid.getInstance()
        val transaction = walletManager.safeCreationAndSendGenesisWallet(
            Coin.valueOf(entranceFee)
        )
        return transaction.transactionId
    }

    /**
     * 1.2 Finishes the last step of creating a shared bitcoin wallet.
     * Posts a self-signed trust chain block containing the shared wallet data.
     */
    public fun broadcastCreatedSharedWallet(
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
     * 2.1 This functions handles the process of proposing to join an existing shared wallet.
     * Create a bitcoin transaction that creates a new shared wallet. This takes some time to complete.
     *
     * NOTE:
     *  - Assumed that the vote passed with enough votes.
     *  - It takes some time before the shared wallet is accepted on the bitcoin blockchain.
     * @param swBlockHash hash of the latest (that you know of) shared wallet block.
     */
    public fun createBitcoinSharedWallet(swBlockHash: ByteArray): WalletManager.TransactionPackage {
        val swJoinBlock: TrustChainBlock =
            getTrustChainCommunity().database.getBlockWithHash(swBlockHash)
                ?: throw IllegalStateException("Shared Wallet not found given the hash: $swBlockHash")

        val blockData = SWJoinBlockTransactionData(swJoinBlock.transaction).getData()
        val walletManager = WalletManagerAndroid.getInstance()

        val oldTransaction = blockData.SW_TRANSACTION_SERIALIZED
        val bitcoinPublicKeys = blockData.SW_BITCOIN_PKS
        bitcoinPublicKeys.add(walletManager.networkPublicECKeyHex())
        val newThreshold =
            SWUtil.percentageToIntThreshold(bitcoinPublicKeys.size, blockData.SW_VOTING_THRESHOLD)

        val newTransactionProposal = walletManager.safeCreationJoinWalletTransaction(
            bitcoinPublicKeys,
            Coin.valueOf(blockData.SW_ENTRANCE_FEE),
            Transaction(walletManager.params, oldTransaction.hexToBytes()),
            newThreshold
        )

        // Ask others for a signature. Assumption:
        // At this point, enough 'yes' votes are received. They will now send their signatures
        return newTransactionProposal
    }

    /**
     * 2.2 Send a proposal on the trust chain to join a shared wallet and to collect signatures.
     * Assumption: enough 'yes' votes, you are allowed to enter the wallet.
     * @param swBlockHash - hash of the latest (that you know of) shared wallet block.
     * @param serializedTransaction - the serialized Bitcoin new shared wallet transaction.
     */
    public fun proposeJoinWalletOnTrustChain(
        swBlockHash: ByteArray,
        serializedTransaction: String
    ): SWSignatureAskTransactionData {
        val swJoinBlock: TrustChainBlock =
            getTrustChainCommunity().database.getBlockWithHash(swBlockHash)
                ?: throw IllegalStateException("Shared Wallet not found given the hash: $swBlockHash")
        val blockData = SWJoinBlockTransactionData(swJoinBlock.transaction).getData()
        val oldTransactionSerialized = blockData.SW_TRANSACTION_SERIALIZED
        val total = blockData.SW_BITCOIN_PKS.size
        val requiredSignatures =
            SWUtil.percentageToIntThreshold(total, blockData.SW_VOTING_THRESHOLD)
        val askSignatureBlockData = SWSignatureAskTransactionData(
            blockData.SW_UNIQUE_ID,
            serializedTransaction,
            oldTransactionSerialized,
            requiredSignatures
        )

        for (swParticipantPk in blockData.SW_TRUSTCHAIN_PKS) {
            trustchain.createProposalBlock(
                askSignatureBlockData.getJsonString(),
                swParticipantPk.hexToBytes(),
                askSignatureBlockData.blockType
            )
        }
        return askSignatureBlockData
    }

    /**
     * 2.3 Add the final shared wallet join block to the trust chain.
     *
     * NOTE:
     * - This function ASSUMES that the user already joined the bitcoin shared wallet
     * - The user should have paid the fee and should have created the new wallet
     * - See `createBitcoinSharedWalletAndProposeOnTrustChain` if this is not the case.
     */
    public fun addSharedWalletJoinBlock(swBlockHash: ByteArray) {
        val swJoinBlock: TrustChainBlock =
            getTrustChainCommunity().database.getBlockWithHash(swBlockHash)
                ?: throw IllegalStateException("Shared Wallet not found given the hash: $swBlockHash")

        val block = SWJoinBlockTransactionData(swJoinBlock.transaction)
        val oldTrustChainPks = block.getData().SW_TRUSTCHAIN_PKS.toMutableList()
        block.addBitcoinPk(WalletManagerAndroid.getInstance().networkPublicECKeyHex())
        block.addTrustChainPk(myPeer.publicKey.keyToBin().toHex())

        for (swParticipantPk in oldTrustChainPks) {
            trustchain.createProposalBlock(
                block.getJsonString(), swParticipantPk.hexToBytes(), block.blockType
            )
        }
    }

    /**
     * 2.4 Last step, commit the join wallet transaction on the bitcoin blockchain.
     *
     * Note:
     * There should be enough sufficient signatures, based on the multisig wallet data.
     */
    fun safeSendingJoinWalletTransaction(
        data: SWSignatureAskTransactionData,
        signatures: List<String>
    ): String {
        val blockData = data.getData()
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
        val newTransaction = walletManager.safeSendingJoinWalletTransaction(
            signaturesOfOldOwners,
            newTransactionProposal,
            oldTransaction
        )
            ?: throw IllegalStateException("Not enough (or faulty) signatures to transfer SW funds")

        return newTransaction.transactionId
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
        swBlockHash: ByteArray,
        receiverAddressSerialized: String,
        satoshiAmount: Long
    ): SWTransferFundsAskTransactionData {
        val swJoinBlock: TrustChainBlock =
            getTrustChainCommunity().database.getBlockWithHash(swBlockHash)
                ?: throw IllegalStateException("Shared Wallet not found given the hash: $swBlockHash")
        val blockData = SWJoinBlockTransactionData(swJoinBlock.transaction).getData()
        val oldTransactionSerialized = blockData.SW_TRANSACTION_SERIALIZED
        val total = blockData.SW_BITCOIN_PKS.size
        val requiredSignatures =
            SWUtil.percentageToIntThreshold(total, blockData.SW_VOTING_THRESHOLD)

        val askSignatureBlockData = SWTransferFundsAskTransactionData(
            blockData.SW_UNIQUE_ID,
            oldTransactionSerialized,
            requiredSignatures,
            satoshiAmount,
            blockData.SW_BITCOIN_PKS,
            receiverAddressSerialized
        )

        for (swParticipantPk in blockData.SW_TRUSTCHAIN_PKS) {
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
        serializedSignatures: List<String>,
        swBlockHash: ByteArray,
        receiverAddress: String,
        satoshiAmount: Long
    ): WalletManager.TransactionPackage {
        val mostRecentSWBlock = fetchLatestSharedWalletTransactionBlock(swBlockHash)
            ?: throw IllegalStateException("Something went wrong fetching the latest SW Block: $swBlockHash")

        val transactionSerialized = tryToFetchSerializedTransaction(mostRecentSWBlock)
            ?: throw IllegalStateException("Invalid most recent SW block found. No serialized transaction!")
        val walletManager = WalletManagerAndroid.getInstance()
        val bitcoinTransaction =
            Transaction(walletManager.params, transactionSerialized.hexToBytes())

        val signatures = serializedSignatures.map {
            ECKey.ECDSASignature.decodeFromDER(it.hexToBytes())
        }

        val sendTransaction = walletManager.safeSendingTransactionFromMultiSig(
            bitcoinTransaction,
            signatures,
            org.bitcoinj.core.Address.fromString(walletManager.params, receiverAddress),
            Coin.valueOf(satoshiAmount)
        ) ?: throw IllegalStateException("Not enough (or faulty) signatures to transfer SW funds")

        return sendTransaction
    }

    /**
     * 3.3 Everything is done, publish the final serialized bitcoin transaction data on trustchain.
     */
    public fun postTransactionSucceededOnTrustChain(
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

        trustchain.createProposalBlock(
            transactionData.getJsonString(),
            myPeer.publicKey.keyToBin(),
            transactionData.blockType
        )
    }

    /**
     * Fetch blocks with type SHARED_WALLET_BLOCK.
     */
    private fun fetchSharedWalletBlocks(): List<TrustChainBlock> {
        return fetchSharedWalletBlocks(listOf(SHARED_WALLET_BLOCK))
    }

    private fun fetchSharedWalletBlocks(blockTypes: List<String>): List<TrustChainBlock> {
        var result = arrayListOf<TrustChainBlock>()
        for (type in blockTypes) {
            result.addAll(getTrustChainCommunity().database.getBlocksWithType(type))
        }
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
    private fun fetchLatestSharedWalletTransactionBlock(swBlockHash: ByteArray): TrustChainBlock? {
        val swBlock = getTrustChainCommunity().database.getBlockWithHash(swBlockHash)
            ?: return null
        val transactionBlocks = fetchSharedWalletBlocks(SW_TRANSACTION_BLOCK_KEYS)
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

            val userTrustchainPks = SWUtil.parseJSONArray(blockData.get(SW_TRUSTCHAIN_PKS).asJsonArray)
            userTrustchainPks.contains(myPeer.publicKey.keyToBin().toHex())
        }
    }

    public fun fetchJoinSignatures(walletId: String, proposalId: String): List<String> {
        return getTrustChainCommunity().database.getBlocksWithType(SIGNATURE_ASK_BLOCK).filter {
            val blockData = SWResponseSignatureTransactionData(it.transaction)
            it.isAgreement && blockData.matchesProposal(walletId, proposalId)
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
        fun joinAskBlockReceived(block: TrustChainBlock) {
            val trustchain = TrustChainHelper(IPv8Android.getInstance().getOverlay() ?: return)

            val blockData = SWSignatureAskTransactionData(block.transaction).getData()
            val walletManager = WalletManagerAndroid.getInstance()

            val oldTransactionSerialized = blockData.SW_TRANSACTION_SERIALIZED_OLD
            val newTransactionSerialized = blockData.SW_TRANSACTION_SERIALIZED
            val signature = walletManager.safeSigningJoinWalletTransaction(
                Transaction(walletManager.params, oldTransactionSerialized.hexToBytes()),
                Transaction(walletManager.params, newTransactionSerialized.hexToBytes()),
                walletManager.protocolECKey()
            )
            val signatureSerialized = signature.encodeToDER().toHex()
            val agreementData = SWResponseSignatureTransactionData(
                blockData.SW_UNIQUE_ID,
                blockData.SW_UNIQUE_PROPOSAL_ID,
                signatureSerialized
            )
            trustchain.createAgreementBlock(block, agreementData.getTransactionData())
        }

        /**
         * Given a shared wallet transfer fund proposal block, calculate the signature and send an agreement block.
         */
        public fun transferFundsBlockReceived(block: TrustChainBlock) {
            val trustchain = TrustChainHelper(IPv8Android.getInstance().getOverlay() ?: return)
            val walletManager = WalletManagerAndroid.getInstance()
            val blockData = SWTransferFundsAskTransactionData(block.transaction).getData()

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
            trustchain.createAgreementBlock(block, agreementData.getTransactionData())
        }

        public const val SHARED_WALLET_BLOCK = "SHARED_WALLET_BLOCK"
        public const val TRANSFER_FINAL_BLOCK = "TRANSFER_FINAL_BLOCK"
        public const val SIGNATURE_ASK_BLOCK = "JOIN_ASK_BLOCK"
        public const val TRANSFER_FUNDS_ASK_BLOCK = "TRANSFER_FUNDS_ASK_BLOCK"
        public const val SIGNATURE_AGREEMENT_BLOCK = "SIGNATURE_AGREEMENT_BLOCK"

        // Values below are present in SW_TRANSACTION_BLOCK_KEYS block types
        public val SW_TRANSACTION_BLOCK_KEYS = listOf(SHARED_WALLET_BLOCK, TRANSFER_FINAL_BLOCK)
        public const val SW_UNIQUE_ID = "SW_UNIQUE_ID"
        public const val SW_TRANSACTION_SERIALIZED = "SW_PK"
        public const val SW_TRUSTCHAIN_PKS = "SW_TRUSTCHAIN_PKS"
    }
}
