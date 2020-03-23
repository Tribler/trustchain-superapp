package nl.tudelft.ipv8.android.demo

import nl.tudelft.ipv8.Community
import nl.tudelft.ipv8.android.IPv8Android
import nl.tudelft.ipv8.android.demo.coin.WalletManager
import nl.tudelft.ipv8.android.demo.coin.WalletManagerAndroid
import nl.tudelft.ipv8.android.demo.sharedWallet.*
import nl.tudelft.ipv8.attestation.trustchain.*
import nl.tudelft.ipv8.util.hexToBytes
import nl.tudelft.ipv8.util.toHex
import org.bitcoinj.core.Coin
import org.bitcoinj.core.ECKey
import org.bitcoinj.core.Transaction

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
     * Create a shared wallet block.
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
     * Finishes the last step of creating a shared bitcoin wallet.
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
     * This functions handles the process of proposing to join an existing shared wallet.
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

        val blockData = SWJoinBlockTransactionData(swJoinBlock.transaction)
        val walletManager = WalletManagerAndroid.getInstance()

        val oldTransaction = blockData.getTransactionSerialized()
        val bitcoinPublicKeys = blockData.getBitcoinPks()
        bitcoinPublicKeys.add(walletManager.networkPublicECKeyHex())
        val newThreshold =
            SWUtil.percentageToIntThreshold(bitcoinPublicKeys.size, blockData.getThreshold())

        val newTransactionProposal = walletManager.safeCreationJoinWalletTransaction(
            bitcoinPublicKeys,
            Coin.valueOf(blockData.getEntranceFee()),
            Transaction(walletManager.params, oldTransaction.hexToBytes()),
            newThreshold
        )

        // Ask others for a signature. Assumption:
        // At this point, enough 'yes' votes are received. They will now send their signatures
        return newTransactionProposal
    }

    /**
     * Send a proposal on the trust chain to join a shared wallet and to collect signatures.
     * Assumption: enough 'yes' votes, you are allowed to enter the wallet.
     * @param swBlockHash - hash of the latest (that you know of) shared wallet block.
     * @param serializedTransaction - the serialized Bitcoin new shared wallet transaction.
     */
    public fun proposeJoinWalletOnTrustChain(
        swBlockHash: ByteArray,
        serializedTransaction: String
    ): SWJoinAskBlockTransactionData {
        val swJoinBlock: TrustChainBlock =
            getTrustChainCommunity().database.getBlockWithHash(swBlockHash)
                ?: throw IllegalStateException("Shared Wallet not found given the hash: $swBlockHash")
        val blockData = SWJoinBlockTransactionData(swJoinBlock.transaction)
        val oldTransactionSerialized = blockData.getTransactionSerialized()
        val total = blockData.getBitcoinPks().size
        val requiredSignatures = SWUtil.percentageToIntThreshold(total, blockData.getThreshold())
        val askSignatureBlockData = SWJoinAskBlockTransactionData(
            blockData.getUniqueId(),
            serializedTransaction,
            oldTransactionSerialized,
            requiredSignatures
        )

        for (swParticipantPk in blockData.getTrustChainPks()) {
            trustchain.createProposalBlock(
                askSignatureBlockData.getJsonString(),
                swParticipantPk.hexToBytes(),
                askSignatureBlockData.blockType
            )
        }
        return askSignatureBlockData
    }

    /**
     * Add the final shared wallet join block to the trust chain.
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

        val blockData = SWJoinBlockTransactionData(swJoinBlock.transaction)
        val oldTrustChainPks = blockData.getTrustChainPks().toMutableList()
        blockData.addBitcoinPk(WalletManagerAndroid.getInstance().networkPublicECKeyHex())
        blockData.addTrustChainPk(myPeer.publicKey.keyToBin().toHex())

        for (swParticipantPk in oldTrustChainPks) {
            trustchain.createProposalBlock(
                blockData.getJsonString(), swParticipantPk.hexToBytes(), blockData.blockType
            )
        }
    }

    /**
     * Transfer funds from an existing shared wallet to a third-party. Broadcast bitcoin transaction.
     */
    public fun transferFunds(
        serializedSignatures: List<String>, swBlockHash: ByteArray,
        receiverAddress: String, satoshiAmount: Long
    ): String {
        val mostRecentSWBlock = fetchLatestSharedWalletBlock(swBlockHash)
            ?: throw IllegalStateException("Something went wrong fetching the latest SW Block: $swBlockHash")

        val blockData = SWJoinBlockTransactionData(mostRecentSWBlock.transaction)
        val walletManager = WalletManagerAndroid.getInstance()
        val bitcoinTransaction =
            Transaction(walletManager.params, blockData.getTransactionSerialized().hexToBytes())

        val signatures = serializedSignatures.map {
            ECKey.ECDSASignature.decodeFromDER(it.hexToBytes())
        }

        val sendTransaction = walletManager.safeSendingTransactionFromMultiSig(
            bitcoinTransaction,
            signatures,
            org.bitcoinj.core.Address.fromString(walletManager.params, receiverAddress),
            Coin.valueOf(satoshiAmount)
        ) ?: throw IllegalStateException("Not enough (or faulty) signatures to transfer SW funds")

        return sendTransaction.transactionId
    }

    public fun askForTransferFundsSignatures() {

    }

    public fun provideTransferFundsSignature(block: TrustChainBlock) {
        // TODO: implement trust chain blocks for this one
        // I will sign a transaction stating that coins will go from a multi-sig to a third-party.
        val transactionSerialized = ""
        val receiverAddressSerialized = ""
        val value = 10L

        val walletManager = WalletManagerAndroid.getInstance()
        val signature = walletManager.safeSigningTransactionFromMultiSig(
            Transaction(walletManager.params, transactionSerialized.hexToBytes()),
            walletManager.protocolECKey(),
            org.bitcoinj.core.Address.fromString(walletManager.params, receiverAddressSerialized),
            Coin.valueOf(value)
        )
        val signaturesSerialized = signature.encodeToDER().toHex()
    }

    /**
     * Fetch blocks with type SHARED_WALLET_BLOCK.
     */
    private fun fetchSharedWalletBlocks(): List<TrustChainBlock> {
        return getTrustChainCommunity().database.getBlocksWithType(SHARED_WALLET_BLOCK)
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
        val walletId = SWUtil.parseTransaction(block.transaction).getString(SW_UNIQUE_ID)
        return fromBlocks
            .filter { SWUtil.parseTransaction(it.transaction).getString(SW_UNIQUE_ID) == walletId }
            .maxBy { it.timestamp.time }
    }

    /**
     * Fetch the latest block associated with a shared wallet.
     * swBlockHash - the hash of one of the blocks associated with a shared wallet.
     */
    private fun fetchLatestSharedWalletBlock(swBlockHash: ByteArray): TrustChainBlock? {
        val swBlock = getTrustChainCommunity().database.getBlockWithHash(swBlockHash)
            ?: return null
        return fetchLatestSharedWalletBlock(swBlock, fetchSharedWalletBlocks())
    }

    /**
     * Discover shared wallets that you can join, return the latest (known) blocks.
     */
    public fun discoverSharedWallets(): List<TrustChainBlock> {
        val sharedWalletBlocks = fetchSharedWalletBlocks()
        // For every distinct unique shared wallet, find the latest block
        return sharedWalletBlocks
            .distinctBy { SWUtil.parseTransaction(it.transaction).getString(SW_UNIQUE_ID) }
            .map { fetchLatestSharedWalletBlock(it, sharedWalletBlocks) ?: it }
    }

    /**
     * Fetch the shared wallet blocks that you are part of, based on your trustchain PK.
     */
    public fun fetchLatestJoinedSharedWalletBlocks(): List<TrustChainBlock> {
        return discoverSharedWallets().filter {
            val blockData = SWUtil.parseTransaction(it.transaction)
            val userTrustchainPks = SWUtil.parseJSONArray(blockData.getJSONArray(SW_TRUSTCHAIN_PKS))
            userTrustchainPks.contains(myPeer.publicKey.keyToBin().toHex())
        }
    }

    public fun fetchJoinSignatures(walletId: String, proposalId: String): List<String> {
        return getTrustChainCommunity().database.getBlocksWithType(JOIN_ASK_BLOCK).filter {
            val blockData = SWResponseSignatureTransactionData(it.transaction)
            it.isAgreement && blockData.matchesProposal(walletId, proposalId)
        }.map {
            val blockData = SWResponseSignatureTransactionData(it.transaction)
            blockData.getSignatureSerialized()
        }
    }

    companion object {
        /**
         * Given a shared wallet proposal block, calculate the signature and send in an agreement block.
         * Called by the listener of the [JOIN_ASK_BLOCK] type. Respond with [SIGNATURE_AGREEMENT_BLOCK].
         */
        fun joinAskBlockReceived(block: TrustChainBlock) {
            val trustchain = TrustChainHelper(IPv8Android.getInstance().getOverlay() ?: return)

            val blockData = SWJoinAskBlockTransactionData(block.transaction)
            val walletManager = WalletManagerAndroid.getInstance()

            val oldTransactionSerialized = blockData.getOldTransactionSerialized()
            val newTransactionSerialized = blockData.getTransactionSerialized()
            val signature = walletManager.safeSigningJoinWalletTransaction(
                Transaction(walletManager.params, oldTransactionSerialized.hexToBytes()),
                Transaction(walletManager.params, newTransactionSerialized.hexToBytes()),
                walletManager.protocolECKey()
            )
            val signatureSerialized = signature.encodeToDER().toHex()
            val agreementData = SWResponseSignatureTransactionData(
                blockData.getUniqueId(),
                blockData.getUniqueProposalId(),
                signatureSerialized
            )
            trustchain.createAgreementBlock(block, agreementData.getTransactionData())
        }

        fun safeSendingJoinWalletTransaction(
            data: SWJoinAskBlockTransactionData,
            signatures: List<String>
        ): String {
            val oldTransactionSerialized = data.getOldTransactionSerialized()
            val newTransactionSerialized = data.getTransactionSerialized()

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

        public const val SHARED_WALLET_BLOCK = "SHARED_WALLET_BLOCK"
        public const val JOIN_ASK_BLOCK = "JOIN_ASK_BLOCK"
        public const val TRANSFER_FUNDS_ASK_BLOCK = "TRANSFER_FUNDS_ASK_BLOCK"
        public const val SIGNATURE_AGREEMENT_BLOCK = "SIGNATURE_AGREEMENT_BLOCK"

        public const val SW_UNIQUE_ID = "SW_UNIQUE_ID"
        public const val SW_UNIQUE_PROPOSAL_ID = "SW_UNIQUE_PROPOSAL_ID"
        public const val SW_ENTRANCE_FEE = "SW_ENTRANCE_FEE"
        public const val SW_TRANSFER_FUNDS_AMOUNT = "SW_TRANSFER_FUNDS_AMOUNT"
        public const val SW_TRANSACTION_SERIALIZED = "SW_PK"
        public const val SW_TRANSACTION_SERIALIZED_OLD = "SW_PK_OLD"
        public const val SW_SIGNATURE_SERIALIZED = "SW_SIGNATURE_SERIALIZED"
        public const val SW_VOTING_THRESHOLD = "SW_VOTING_THRESHOLD"
        public const val SW_SIGNATURES_REQUIRED = "SW_SIGNATURES_REQUIRED"
        public const val SW_TRUSTCHAIN_PKS = "SW_TRUSTCHAIN_PKS"
        public const val SW_BITCOIN_PKS = "SW_BLOCKCHAIN_PKS"
    }
}
