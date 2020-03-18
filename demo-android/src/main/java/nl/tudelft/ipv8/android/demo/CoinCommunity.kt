package nl.tudelft.ipv8.android.demo
import android.util.Log
import nl.tudelft.ipv8.Address
import nl.tudelft.ipv8.Community
import nl.tudelft.ipv8.IPv8
import nl.tudelft.ipv8.android.IPv8Android
import nl.tudelft.ipv8.android.demo.coin.CoinUtil
import nl.tudelft.ipv8.android.demo.coin.WalletManager
import nl.tudelft.ipv8.android.demo.coin.WalletManagerAndroid
import nl.tudelft.ipv8.attestation.trustchain.*
import nl.tudelft.ipv8.peerdiscovery.DiscoveryCommunity
import nl.tudelft.ipv8.util.hexToBytes
import nl.tudelft.ipv8.util.toHex
import org.bitcoinj.core.Coin
import org.json.JSONObject
import java.util.*
import kotlin.collections.ArrayList

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

        val transaction = walletManager.startNewWalletProcess(
            listOf(bitcoinPublicKey),
            Coin.valueOf(entranceFee),
            1
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

    public fun joinSharedWallet(swBlockHash: ByteArray, bitcoinPk: ByteArray) {
        val swJoinBlock: TrustChainBlock = getTrustChainCommunity().database.getBlockWithHash(swBlockHash)
            ?: throw IllegalStateException("Shared Wallet not found given the hash: $swBlockHash")

        val parsedTransaction = CoinUtil.parseTransaction(swJoinBlock.transaction)
        val oldTrustchainPks = CoinUtil.parseJSONArray(parsedTransaction.getJSONArray(SW_TRUSTCHAIN_PKS))
        // TODO: Pay the entrance fee with bitcoinPk
        // TODO: Create new shared wallet using bitcoinPks

        val newTrustchainPks: ArrayList<String> = arrayListOf()
        newTrustchainPks.addAll(oldTrustchainPks)
        newTrustchainPks.add(myPeer.publicKey.keyToBin().toHex())

        val newBitcoinPks: ArrayList<String> = CoinUtil.parseJSONArray(parsedTransaction.getJSONArray(SW_BITCOIN_PKS))
        newBitcoinPks.add(bitcoinPk.toHex())

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

        // TODO: TIMEOUT, wait for votes, collect key parts
    }

    public fun transferFunds(oldSwPk: ByteArray, newSwPk: ByteArray) {
        // TODO: send funds to new wallet
    }

    private fun fetchSharedWalletBlocks(): List<TrustChainBlock> {
        return getTrustChainCommunity().database.getBlocksWithType(SHARED_WALLET_BLOCK)
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

    companion object {
        public const val SHARED_WALLET_BLOCK = "SHARED_WALLET_BLOCK"
        public const val SW_UNIQUE_ID = "SW_UNIQUE_ID"
        public const val SW_ENTRANCE_FEE = "SW_ENTRANCE_FEE"
        public const val SW_TRANSACTION_SERIALIZED = "SW_PK"
        public const val SW_VOTING_THRESHOLD = "SW_VOTING_THRESHOLD"
        public const val SW_TRUSTCHAIN_PKS = "SW_TRUSTCHAIN_PKS"
        public const val SW_BITCOIN_PKS = "SW_BLOCKCHAIN_PKS"
    }
}
