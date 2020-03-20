package nl.tudelft.ipv8.android.demo

import nl.tudelft.ipv8.Community
import nl.tudelft.ipv8.android.IPv8Android
import nl.tudelft.ipv8.android.demo.sharedWallet.SWUtil
import nl.tudelft.ipv8.android.demo.coin.WalletManagerAndroid
import nl.tudelft.ipv8.attestation.trustchain.*
import nl.tudelft.ipv8.util.hexToBytes
import nl.tudelft.ipv8.util.toHex
import nl.tudelft.ipv8.Address
import nl.tudelft.ipv8.android.demo.sharedWallet.SWJoinAskBlockTransactionData
import nl.tudelft.ipv8.android.demo.sharedWallet.SWJoinBlockTransactionData
import org.bitcoinj.core.Coin
import org.bitcoinj.core.ECKey
import org.bitcoinj.core.Transaction
import java.util.*

@Suppress("UNCHECKED_CAST")
class CoinCommunity : Community() {
    override val serviceId = "0000bitcoin0000community0000"

    private val discoveredAddressesContacted: MutableMap<Address, Date> = mutableMapOf()
    private val trustchain: TrustChainHelper by lazy {
        TrustChainHelper(getTrustChainCommunity())
    }

    override fun walkTo(address: Address) {
        super.walkTo(address)
        discoveredAddressesContacted[address] = Date()
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
     * 1. Create a transaction that creates a new shared wallet. This takes some time to complete.
     * 2. Send the proposal on the trust chain to ask for signatures.
     *
     * NOTE:
     *  - Assumed that the vote passed with enough votes.
     *  - You have to wait a bit before it is possible to call `addSharedWalletJoinBlock`
     *  - This function only proposes, it does not add the user to a shared wallet
     *    (see `addSharedWalletJoinBlock` for that functionality)
     */
    public fun createBitcoinSharedWalletAndProposeOnTrustChain(swBlockHash: ByteArray): String {
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
        val serializedTransaction = newTransactionProposal.tx.bitcoinSerialize().toHex()
        val askSignatureBlockData = SWJoinAskBlockTransactionData(
            blockData.getUniqueId(), serializedTransaction, oldTransaction
        )

        for (swParticipantPk in blockData.getTrustChainPks()) {
            trustchain.createProposalBlock(
                askSignatureBlockData.getJsonString(),
                swParticipantPk.hexToBytes(),
                askSignatureBlockData.blockType
            )
        }

        return serializedTransaction
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

    public fun transferFunds(
        serializedSignatures: List<String>, swBlockHash: ByteArray,
        receiverAddress: String, satoshiAmount: Long
    ): String {
        // We (together) want to send coins to a third-party.
        // I have received all signatures for this.
        // I will broadcast the transaction.
        val mostRecentSWBlock = fetchLatestSharedWalletBlock(swBlockHash)
            ?: throw IllegalStateException("Something went wrong fetching the latest SW Block: $swBlockHash")
        val blockData = SWUtil.parseTransaction(mostRecentSWBlock.transaction)
        val serializedBitcoinTransaction = blockData.getString(SW_TRANSACTION_SERIALIZED)

        val walletManager = WalletManagerAndroid.getInstance()
        val bitcoinTransaction =
            Transaction(walletManager.params, serializedBitcoinTransaction.hexToBytes())
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

    public fun provideTransferFundsSignature(block: TrustChainBlock) {
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

    private fun fetchSharedWalletBlocks(): List<TrustChainBlock> {
        return getTrustChainCommunity().database.getBlocksWithType(SHARED_WALLET_BLOCK)
    }

    private fun fetchLatestSharedWalletBlock(swBlockHash: ByteArray): TrustChainBlock? {
        val swBlock: TrustChainBlock =
            getTrustChainCommunity().database.getBlockWithHash(swBlockHash)
                ?: return null
        return fetchLatestSharedWalletBlock(swBlock, fetchSharedWalletBlocks())
    }

    /**
     * Fetch the latest shared wallet block, based on a given block 'block'.
     * The unique shared wallet id is used to find the most recent block in
     * the 'sharedWalletBlocks' list.
     */
    private fun fetchLatestSharedWalletBlock(
        block: TrustChainBlock,
        sharedWalletBlocks: List<TrustChainBlock>
    )
        : TrustChainBlock? {
        val walletId = SWUtil.parseTransaction(block.transaction).getString(SW_UNIQUE_ID)
        return sharedWalletBlocks
            .filter { SWUtil.parseTransaction(it.transaction).getString(SW_UNIQUE_ID) == walletId }
            .maxBy { it.timestamp.time }
    }

    /**
     * Discover shared wallets that you can join, return the latest (known) blocks
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

    public fun countJoinSignaturesReceived(sqUniqueId: String) {
        for (block in getTrustChainCommunity().database.getBlocksWithType(SHARED_WALLET_BLOCK)) {
        }

    }

    companion object {
        fun joinAskBlockReceived(block: TrustChainBlock) {
            val trustchain = TrustChainHelper(IPv8Android.getInstance().getOverlay() ?: return)

            val transactionData = SWUtil.parseTransaction(block.transaction)
            val oldTransactionSerialized = transactionData.getString(SW_TRANSACTION_SERIALIZED_OLD)
            val newTransactionSerialized = transactionData.getString(SW_TRANSACTION_SERIALIZED)

            val walletManager = WalletManagerAndroid.getInstance()
            val signature = walletManager.safeSigningJoinWalletTransaction(
                Transaction(walletManager.params, oldTransactionSerialized.hexToBytes()),
                Transaction(walletManager.params, newTransactionSerialized.hexToBytes()),
                walletManager.protocolECKey()
            )
            val signatureSerialized = signature.encodeToDER().toHex()
            val transactionValues = mapOf(
                SW_UNIQUE_ID to transactionData.getString(SW_UNIQUE_ID),
                SW_SIGNATURE_SERIALIZED to signatureSerialized
            )
            val transaction = mapOf("message" to transactionValues)

            trustchain.createAgreementBlock(block, transaction)
        }

        fun safeSendingJoinWalletTransaction(): String {
            // I am proposer. I want to join. I have gotten enough signatures. I will broadcast on bitcoin.
            val signaturesOfOldOwnersSerialized = listOf<String>()
            val oldTransactionSerialized = ""
            val newTransactionSerialized = ""

            val walletManager = WalletManagerAndroid.getInstance()

            val signaturesOfOldOwners = signaturesOfOldOwnersSerialized.map {
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
        public const val SW_UNIQUE_ID = "SW_UNIQUE_ID"
        public const val SW_ENTRANCE_FEE = "SW_ENTRANCE_FEE"
        public const val SW_TRANSACTION_SERIALIZED = "SW_PK"
        public const val SW_TRANSACTION_SERIALIZED_OLD = "SW_PK_OLD"
        public const val SW_SIGNATURE_SERIALIZED = "SW_SIGNATURE_SERIALIZED"
        public const val SW_VOTING_THRESHOLD = "SW_VOTING_THRESHOLD"
        public const val SW_TRUSTCHAIN_PKS = "SW_TRUSTCHAIN_PKS"
        public const val SW_BITCOIN_PKS = "SW_BLOCKCHAIN_PKS"
    }
}
