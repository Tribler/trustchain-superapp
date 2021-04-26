package nl.tudelft.trustchain.currencyii

import nl.tudelft.ipv8.Community
import nl.tudelft.ipv8.android.IPv8Android
import nl.tudelft.ipv8.attestation.trustchain.TrustChainBlock
import nl.tudelft.ipv8.attestation.trustchain.TrustChainCommunity
import nl.tudelft.ipv8.attestation.trustchain.TrustChainTransaction
import nl.tudelft.ipv8.util.hexToBytes
import nl.tudelft.ipv8.util.toHex
import nl.tudelft.trustchain.currencyii.sharedWallet.*
import nl.tudelft.trustchain.currencyii.util.DAOCreateHelper
import nl.tudelft.trustchain.currencyii.util.DAOJoinHelper
import nl.tudelft.trustchain.currencyii.util.DAOTransferFundsHelper
import org.bitcoinj.core.Transaction

@Suppress("UNCHECKED_CAST")
class CoinCommunity : Community() {
    override val serviceId = "02313685c1912a141279f8248fc8db5899c5df5b"

    private fun getTrustChainCommunity(): TrustChainCommunity {
        return IPv8Android.getInstance().getOverlay()
            ?: throw IllegalStateException("TrustChainCommunity is not configured")
    }

    private val daoCreateHelper = DAOCreateHelper()
    private val daoJoinHelper = DAOJoinHelper()
    private val daoTransferFundsHelper = DAOTransferFundsHelper()

    /**
     * Create a bitcoin genesis wallet and broadcast the result on trust chain.
     * The bitcoin transaction may take some time to finish.
     * **Throws** exceptions if something goes wrong with creating or broadcasting bitcoin transaction.
     */
    public fun createBitcoinGenesisWallet(
        entranceFee: Long,
        threshold: Int,
        progressCallback: ((progress: Double) -> Unit)? = null,
        timeout: Long = DEFAULT_BITCOIN_MAX_TIMEOUT
    ) {
        daoCreateHelper.createBitcoinGenesisWallet(
            myPeer,
            entranceFee,
            threshold,
            progressCallback,
            timeout
        )
    }

    /**
     * 2.1 Send a proposal on the trust chain to join a shared wallet and to collect signatures.
     * The proposal is a serialized bitcoin join transaction.
     * **NOTE** the latest walletBlockData should be given, otherwise the serialized transaction is invalid.
     * @param walletBlock - the latest (that you know of) shared wallet block.
     */
    public fun proposeJoinWallet(
        walletBlock: TrustChainBlock
    ): SWSignatureAskTransactionData {
        return daoJoinHelper.proposeJoinWallet(myPeer, walletBlock)
    }

    /**
     * 2.2 Commit the join wallet transaction on the bitcoin blockchain and broadcast the result on trust chain.
     *
     * Note:
     * There should be enough sufficient signatures, based on the multisig wallet data.
     * **Throws** exceptions if something goes wrong with creating or broadcasting bitcoin transaction.
     */
    public fun joinBitcoinWallet(
        walletBlockData: TrustChainTransaction,
        blockData: SWSignatureAskBlockTD,
        signatures: List<String>,
        progressCallback: ((progress: Double) -> Unit)? = null,
        timeout: Long = DEFAULT_BITCOIN_MAX_TIMEOUT
    ) {
        daoJoinHelper.joinBitcoinWallet(
            myPeer,
            walletBlockData,
            blockData,
            signatures,
            progressCallback,
            timeout
        )
    }

    /**
     * 3.1 Send a proposal block on trustchain to ask for the signatures.
     * Assumed that people agreed to the transfer.
     */
    public fun proposeTransferFunds(
        mostRecentWallet: TrustChainBlock,
        receiverAddressSerialized: String,
        satoshiAmount: Long
    ): SWTransferFundsAskTransactionData {
        return daoTransferFundsHelper.proposeTransferFunds(
            myPeer,
            mostRecentWallet,
            receiverAddressSerialized,
            satoshiAmount
        )
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
        daoTransferFundsHelper.transferFunds(
            myPeer,
            transferFundsData,
            walletData,
            serializedSignatures,
            receiverAddress,
            satoshiAmount,
            progressCallback,
            timeout
        )
    }

    /**
     * Discover shared wallets that you can join, return the latest blocks that the user knows of.
     */
    public fun discoverSharedWallets(): List<TrustChainBlock> {
        val swBlocks = getTrustChainCommunity().database.getBlocksWithType(JOIN_BLOCK)
        return swBlocks
            .distinctBy { SWJoinBlockTransactionData(it.transaction).getData().SW_UNIQUE_ID }
            .map { fetchLatestSharedWalletBlock(it, swBlocks) ?: it }
    }

    /**
     * Discover shared wallets that you can join, return the latest (known) blocks
     * Fetch the latest block associated with a shared wallet.
     * swBlockHash - the hash of one of the blocks associated with a shared wallet.
     */
    public fun fetchLatestSharedWalletBlock(swBlockHash: ByteArray): TrustChainBlock? {
        val swBlock = getTrustChainCommunity().database.getBlockWithHash(swBlockHash)
            ?: return null
        val swBlocks = getTrustChainCommunity().database.getBlocksWithType(JOIN_BLOCK)
        return fetchLatestSharedWalletBlock(swBlock, swBlocks)
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
        if (block.type != JOIN_BLOCK) {
            return null
        }
        val walletId = SWJoinBlockTransactionData(block.transaction).getData().SW_UNIQUE_ID

        return fromBlocks
            .filter { it.type == JOIN_BLOCK } // make sure the blocks have the correct type!
            .filter { SWJoinBlockTransactionData(it.transaction).getData().SW_UNIQUE_ID == walletId }
            .maxByOrNull { it.timestamp.time }
    }

    /**
     * Fetch the shared wallet blocks that you are part of, based on your trustchain PK.
     */
    public fun fetchLatestJoinedSharedWalletBlocks(): List<TrustChainBlock> {
        return discoverSharedWallets().filter {
            val blockData = SWJoinBlockTransactionData(it.transaction).getData()
            val userTrustchainPks = blockData.SW_TRUSTCHAIN_PKS
            userTrustchainPks.contains(myPeer.publicKey.keyToBin().toHex())
        }
    }

    public fun fetchSignatureRequestReceiver(block: TrustChainBlock): String {
        if (block.type == SIGNATURE_ASK_BLOCK) {
            return SWSignatureAskTransactionData(block.transaction).getData().SW_RECEIVER_PK
        }

        if (block.type == TRANSFER_FUNDS_ASK_BLOCK) {
            return SWTransferFundsAskTransactionData(block.transaction).getData().SW_RECEIVER_PK
        }

        return "invalid-pk"
    }

    public fun fetchSignatureRequestProposalId(block: TrustChainBlock): String {
        if (block.type == SIGNATURE_ASK_BLOCK) {
            return SWSignatureAskTransactionData(block.transaction).getData().SW_UNIQUE_PROPOSAL_ID
        }
        if (block.type == TRANSFER_FUNDS_ASK_BLOCK) {
            return SWTransferFundsAskTransactionData(block.transaction).getData()
                .SW_UNIQUE_PROPOSAL_ID
        }

        return "invalid-proposal-id"
    }

    /**
     * Fetch all join and transfer proposals in descending timestamp order.
     * Speed assumption: each proposal has a unique proposal ID (distinct by unique proposal id,
     * without taking the unique wallet id into account).
     */
    public fun fetchProposalBlocks(): List<TrustChainBlock> {
        val joinProposals = getTrustChainCommunity().database.getBlocksWithType(SIGNATURE_ASK_BLOCK)
        val transferProposals = getTrustChainCommunity().database.getBlocksWithType(
            TRANSFER_FUNDS_ASK_BLOCK
        )
        return joinProposals
            .union(transferProposals)
            .filter { fetchSignatureRequestReceiver(it) == myPeer.publicKey.keyToBin().toHex() }
            .distinctBy { fetchSignatureRequestProposalId(it) }
            .sortedByDescending { it.timestamp }
    }

    /**
     * Fetch all DAO blocks that contain a signature. These blocks are the response of a signature request.
     * Signatures are fetched from [SIGNATURE_AGREEMENT_BLOCK] type blocks.
     */
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

    /**
     * Given a shared wallet proposal block, calculate the signature and respond with a trust chain block.
     */
    public fun joinAskBlockReceived(
        block: TrustChainBlock,
        myPublicKey: ByteArray
    ) {
        val latestHash = SWSignatureAskTransactionData(block.transaction).getData()
            .SW_PREVIOUS_BLOCK_HASH
        val mostRecentSWBlock = fetchLatestSharedWalletBlock(latestHash.hexToBytes())
            ?: throw IllegalStateException("Most recent DAO block not found")
        val oldTransaction = SWJoinBlockTransactionData(mostRecentSWBlock.transaction).getData()
            .SW_TRANSACTION_SERIALIZED

        DAOJoinHelper.joinAskBlockReceived(oldTransaction, block, myPublicKey)
    }

    /**
     * Given a shared wallet transfer fund proposal block, calculate the signature and respond with a trust chain block.
     */
    public fun transferFundsBlockReceived(block: TrustChainBlock, myPublicKey: ByteArray) {
        val latestHash = SWTransferFundsAskTransactionData(block.transaction).getData()
            .SW_PREVIOUS_BLOCK_HASH
        val mostRecentSWBlock = fetchLatestSharedWalletBlock(latestHash.hexToBytes())
            ?: throw IllegalStateException("Most recent DAO block not found")
        val oldTransaction = SWJoinBlockTransactionData(mostRecentSWBlock.transaction).getData()
            .SW_TRANSACTION_SERIALIZED

        DAOTransferFundsHelper.transferFundsBlockReceived(oldTransaction, block, myPublicKey)
    }

    companion object {
        /**
         * Helper method that serializes a bitcoin transaction to a string.
         */
        public fun getSerializedTransaction(transaction: Transaction): String {
            return transaction.bitcoinSerialize().toHex()
        }

        // Default maximum wait timeout for bitcoin transaction broadcasts in seconds
        public const val DEFAULT_BITCOIN_MAX_TIMEOUT: Long = 60 * 5

        // Block type for join DAO blocks
        public const val JOIN_BLOCK = "v1DAO_JOIN"

        // Block type for transfer funds (from a DAO)
        public const val TRANSFER_FINAL_BLOCK = "v1DAO_TRANSFER_FINAL"

        // Block type for basic signature requests
        public const val SIGNATURE_ASK_BLOCK = "v1DAO_ASK_SIGNATURE"

        // Block type for transfer funds signature requests
        public const val TRANSFER_FUNDS_ASK_BLOCK = "v1DAO_TRANSFER_ASK_SIGNATURE"

        // Block type for responding to a signature request with a (should be valid) signature
        public const val SIGNATURE_AGREEMENT_BLOCK = "v1DAO_SIGNATURE_AGREEMENT"
    }
}
