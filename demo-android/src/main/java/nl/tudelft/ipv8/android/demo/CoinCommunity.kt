package nl.tudelft.ipv8.android.demo
import nl.tudelft.ipv8.Community
import nl.tudelft.ipv8.IPv8
import nl.tudelft.ipv8.android.IPv8Android
import nl.tudelft.ipv8.android.demo.coin.CoinUtil
import nl.tudelft.ipv8.android.demo.coin.WalletManagerAndroid
import nl.tudelft.ipv8.attestation.trustchain.*
import nl.tudelft.ipv8.util.hexToBytes
import nl.tudelft.ipv8.util.toHex
import nl.tudelft.ipv8.Address
import org.bitcoinj.core.Coin
import org.bitcoinj.core.ECKey
import org.bitcoinj.core.Transaction
import org.json.JSONObject
import java.util.*
import kotlin.collections.ArrayList
import kotlin.math.ceil
import kotlin.math.min

@Suppress("UNCHECKED_CAST")
class CoinCommunity: Community() {
    override val serviceId = "0000bitcoin0000community0000"

    public val discoveredAddressesContacted: MutableMap<Address, Date> = mutableMapOf()

    protected val trustchain: TrustChainHelper by lazy {
        TrustChainHelper(getTrustChainCommunity())
    }

    override fun walkTo(address: Address) {
        super.walkTo(address)
        discoveredAddressesContacted[address] = Date()
    }

    protected fun getTrustChainCommunity(): TrustChainCommunity {
        return getIpv8().getOverlay() ?: throw IllegalStateException("TrustChainCommunity is not configured")
    }

    protected fun getIpv8(): IPv8 {
        return IPv8Android.getInstance()
    }

    private fun createTransactionData(entranceFee: Long, transactionSerialized: String,
                                      votingThreshold: Int, trustchainPks: List<String>,
                                      bitcoinPks: List<String>, uniqueId: String? = null): String {
        val transactionValues = mapOf(
            SW_UNIQUE_ID to (uniqueId ?: CoinUtil.randomUUID()),
            SW_ENTRANCE_FEE to entranceFee,
            SW_TRANSACTION_SERIALIZED to transactionSerialized,
            SW_VOTING_THRESHOLD to votingThreshold,
            SW_TRUSTCHAIN_PKS to trustchainPks,
            SW_BITCOIN_PKS to bitcoinPks
        )
        return JSONObject(transactionValues).toString()
    }

    /**
     * Create a shared wallet block.
     * entranceFee - the fee that has to be paid for new participants
     */
    public fun createSharedWallet(entranceFee: Long): String {
        val walletManager = WalletManagerAndroid.getInstance()
        val bitcoinPublicKey = walletManager.networkPublicECKeyHex()

        val transaction = walletManager.safeCreationAndSendGenesisWallet(
            Coin.valueOf(entranceFee)
        )

        return transaction.transactionId
    }

    public fun tryToSerializeWallet(transactionId: String, entranceFee: Long, votingThreshold: Int): Boolean {
        val walletManager = WalletManagerAndroid.getInstance()
        val transactionSerialized = walletManager.attemptToGetTransactionAndSerialize(transactionId)

        if (transactionSerialized == null) {
            return false
        }

        val bitcoinPublicKey = walletManager.networkPublicECKeyHex()
        val trustchainPk = myPeer.publicKey.keyToBin()

        val values = createTransactionData(
            entranceFee,
            transactionSerialized,
            votingThreshold,
            arrayListOf(trustchainPk.toHex()),
            arrayListOf(bitcoinPublicKey)
        )

        trustchain.createProposalBlock(values, trustchainPk, SHARED_WALLET_BLOCK)
        return true
    }

    public fun joinSharedWallet(swBlockHash: ByteArray): String {
        val swJoinBlock: TrustChainBlock = getTrustChainCommunity().database.getBlockWithHash(swBlockHash)
            ?: throw IllegalStateException("Shared Wallet not found given the hash: $swBlockHash")

        val parsedTransaction = CoinUtil.parseTransaction(swJoinBlock.transaction)
        val oldTrustchainPks = CoinUtil.parseJSONArray(parsedTransaction.getJSONArray(SW_TRUSTCHAIN_PKS))
        val bitcoinPublicKeys = CoinUtil.parseJSONArray(parsedTransaction.getJSONArray(SW_BITCOIN_PKS))
        val entranceFee = parsedTransaction.getLong(SW_ENTRANCE_FEE)
        val oldTransaction = parsedTransaction.getString(SW_TRANSACTION_SERIALIZED)

        val walletManager = WalletManagerAndroid.getInstance()
        val myBitcoinPublicKey = walletManager.networkPublicECKeyHex()
        bitcoinPublicKeys.add(myBitcoinPublicKey)

        val totalAmount = bitcoinPublicKeys.size.toDouble()
        val oldThreshold = parsedTransaction.getInt(SW_VOTING_THRESHOLD).toDouble()
        var threshold = ceil((oldThreshold / 100.0) * totalAmount).toInt()
        val newThreshold = min(bitcoinPublicKeys.size, threshold)

        val newTransactionProposal = walletManager.safeCreationJoinWalletTransaction(
            bitcoinPublicKeys,
            Coin.valueOf(entranceFee),
            Transaction(walletManager.params, oldTransaction.hexToBytes()),
            newThreshold
        )

        // Ask others for a signature
        // At this point, enough 'yes' votes are received. They will now send their signatures
        val serializedTransaction = newTransactionProposal.tx.bitcoinSerialize().toHex()
        val transactionValues = JSONObject(mapOf(
            SW_TRANSACTION_SERIALIZED to serializedTransaction,
            SW_TRANSACTION_SERIALIZED_OLD to oldTransaction
        )).toString()

        for (swParticipantPk in oldTrustchainPks) {
            trustchain.createProposalBlock(transactionValues, swParticipantPk.hexToBytes(), JOIN_ASK_BLOCK)
        }

        return serializedTransaction
    }

    /**
     * Function that can be called to add a user to a shared wallet, on the trustchain.
     */
    public fun addSharedWalletJoinBlock(swBlockHash: ByteArray) {
        val swJoinBlock: TrustChainBlock = getTrustChainCommunity().database.getBlockWithHash(swBlockHash)
            ?: throw IllegalStateException("Shared Wallet not found given the hash: $swBlockHash")

        val parsedTransaction = CoinUtil.parseTransaction(swJoinBlock.transaction)
        val oldTrustchainPks = CoinUtil.parseJSONArray(parsedTransaction.getJSONArray(SW_TRUSTCHAIN_PKS))

        val newTrustchainPks: ArrayList<String> = arrayListOf()
        newTrustchainPks.addAll(oldTrustchainPks)
        newTrustchainPks.add(myPeer.publicKey.keyToBin().toHex())

        val newBitcoinPks: ArrayList<String> = CoinUtil.parseJSONArray(parsedTransaction.getJSONArray(SW_BITCOIN_PKS))
        val walletManager = WalletManagerAndroid.getInstance()
        val myBitcoinPublicKey = walletManager.networkPublicECKeyHex()

        newBitcoinPks.add(myBitcoinPublicKey)

        val values = createTransactionData(
            parsedTransaction.getLong(SW_ENTRANCE_FEE),
            parsedTransaction.getString(SW_TRANSACTION_SERIALIZED),
            parsedTransaction.getInt(SW_VOTING_THRESHOLD),
            newTrustchainPks,
            newBitcoinPks,
            parsedTransaction.getString(SW_UNIQUE_ID)
        )

        for (swParticipantPk in oldTrustchainPks) {
            trustchain.createProposalBlock(values, swParticipantPk.hexToBytes(), SHARED_WALLET_BLOCK)
        }
    }

    public fun transferFunds(serializedSignatures: List<String>, swBlockHash: ByteArray,
                             receiverAddress: String, satoshiAmount: Long): String {
        // We (together) want to send coins to a third-party.
        // I have received all signatures for this.
        // I will broadcast the transaction.
        val mostRecentSWBlock = fetchLatestSharedWalletBlock(swBlockHash)
            ?: throw IllegalStateException("Something went wrong fetching the latest SW Block: $swBlockHash")
        val blockData = CoinUtil.parseTransaction(mostRecentSWBlock.transaction)
        val serializedBitcoinTransaction = blockData.getString(SW_TRANSACTION_SERIALIZED)

        val walletManager = WalletManagerAndroid.getInstance()
        val bitcoinTransaction = Transaction(walletManager.params, serializedBitcoinTransaction.hexToBytes())
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

    public fun fetchBitcoinTransactionStatus(transactionId: String): Boolean {
        val walletManager = WalletManagerAndroid.getInstance()
        val transactionSerialized = walletManager.attemptToGetTransactionAndSerialize(transactionId)
        return transactionSerialized != null
    }

    private fun fetchSharedWalletBlocks(): List<TrustChainBlock> {
        return getTrustChainCommunity().database.getBlocksWithType(SHARED_WALLET_BLOCK)
    }

    private fun fetchLatestSharedWalletBlock(swBlockHash: ByteArray): TrustChainBlock? {
        val swBlock: TrustChainBlock = getTrustChainCommunity().database.getBlockWithHash(swBlockHash)
            ?: return null
        return fetchLatestSharedWalletBlock(swBlock, fetchSharedWalletBlocks())
    }

    /**
     * Fetch the latest shared wallet block, based on a given block 'block'.
     * The unique shared wallet id is used to find the most recent block in
     * the 'sharedWalletBlocks' list.
     */
    private fun fetchLatestSharedWalletBlock(block: TrustChainBlock, sharedWalletBlocks: List<TrustChainBlock>)
        : TrustChainBlock? {
        val walletId = CoinUtil.parseTransaction(block.transaction).getString(SW_UNIQUE_ID)
        return sharedWalletBlocks
            .filter{ CoinUtil.parseTransaction(it.transaction).getString(SW_UNIQUE_ID) == walletId }
            .maxBy { it.timestamp.time }
    }

    /**
     * Discover shared wallets that you can join, return the latest (known) blocks
     */
    public fun discoverSharedWallets(): List<TrustChainBlock> {
        val sharedWalletBlocks = fetchSharedWalletBlocks()
        // For every distinct unique shared wallet, find the latest block
        return sharedWalletBlocks
            .distinctBy { CoinUtil.parseTransaction(it.transaction).getString(SW_UNIQUE_ID) }
            .map { fetchLatestSharedWalletBlock(it, sharedWalletBlocks) ?: it }
    }

    /**
     * Fetch the shared wallet blocks that you are part of, based on your trustchain PK.
     */
    public fun fetchLatestJoinedSharedWalletBlocks(): List<TrustChainBlock> {
        return discoverSharedWallets().filter {
            val blockData = CoinUtil.parseTransaction(it.transaction)
            val userTrustchainPks = CoinUtil.parseJSONArray(blockData.getJSONArray(SW_TRUSTCHAIN_PKS))
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

            val transactionData = CoinUtil.parseTransaction(block.transaction)
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
            val newTransactionProposal = Transaction(walletManager.params, newTransactionSerialized.hexToBytes())
            val oldTransaction = Transaction(walletManager.params, oldTransactionSerialized.hexToBytes())
            val newTransaction = walletManager.safeSendingJoinWalletTransaction(
                signaturesOfOldOwners,
                newTransactionProposal,
                oldTransaction
            ) ?: throw IllegalStateException("Not enough (or faulty) signatures to transfer SW funds")

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
