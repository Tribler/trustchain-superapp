package nl.tudelft.ipv8.android.demo
import nl.tudelft.ipv8.Address
import nl.tudelft.ipv8.Community
import nl.tudelft.ipv8.IPv8
import nl.tudelft.ipv8.android.IPv8Android
import nl.tudelft.ipv8.android.demo.coin.CoinUtil
import nl.tudelft.ipv8.attestation.trustchain.*
import nl.tudelft.ipv8.util.hexToBytes
import nl.tudelft.ipv8.util.toHex
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

    /**
     * Create a shared wallet block
     * entranceFee - the fee that has to be paid for new participants
     * votingThreshold - percentage of voters that need to agree on a new participant to join
     * bitcoinPk - bitcoin public key that is used to pay the initial entrance fee
     */
    public fun createSharedWallet(entranceFee: Double, votingThreshold: Int, bitcoinPk: ByteArray) {
        if (votingThreshold <= 0 || votingThreshold > 100) {
            throw IllegalStateException("The voting threshold (%) for a shared wallet should be [0,100>")
        }

        val trustchainPk = myPeer.publicKey.keyToBin()
        val sharedWalletPK = ByteArray(2)

        // TODO: Create bitcoin wallet
        // TODO: Fill wallet with entrance fee

        val transactionValues = mapOf(
            SW_UNIQUE_ID to CoinUtil.randomUUID(),
            SW_ENTRANCE_FEE to entranceFee,
            SW_PK to sharedWalletPK.toHex(),
            SW_VOTING_THRESHOLD to votingThreshold,
            SW_TRUSTCHAIN_PKS to arrayListOf(trustchainPk.toHex()),
            SW_BITCOIN_PKS to arrayListOf(bitcoinPk.toHex())
        )
        val values = JSONObject(transactionValues).toString()
        trustchain.createProposalBlock(values, trustchainPk, SHARED_WALLET_BLOCK)
    }

    public fun joinSharedWallet(swBlockHash: ByteArray, bitcoinPk: ByteArray) {
        val swJoinBlock: TrustChainBlock = getTrustChainCommunity().database.getBlockWithHash(swBlockHash)
            ?: throw IllegalStateException("Shared Wallet not found given the hash: $swBlockHash")

        val parsedTransaction = CoinUtil.parseTransaction(swJoinBlock.transaction)

        val oldTrustchainPks = CoinUtil.parseJSONArray(parsedTransaction.getJSONArray(SW_TRUSTCHAIN_PKS))
        val oldBitcoinPks = CoinUtil.parseJSONArray(parsedTransaction.getJSONArray(SW_BITCOIN_PKS))
        val trustchainPk = myPeer.publicKey.keyToBin()
        // TODO: Pay the entrance fee with bitcoinPk
        // TODO: Create new shared wallet using bitcoinPks

        val newTrustchainPks: ArrayList<String> = arrayListOf()
        newTrustchainPks.addAll(oldTrustchainPks)
        newTrustchainPks.add(trustchainPk.toHex())

        val newBitcoinPks: ArrayList<String> = arrayListOf()
        newBitcoinPks.addAll(oldBitcoinPks)
        newBitcoinPks.add(bitcoinPk.toHex())

        val transaction = mapOf(
            SW_UNIQUE_ID to parsedTransaction.getString(SW_UNIQUE_ID),
            SW_ENTRANCE_FEE to parsedTransaction.getDouble(SW_ENTRANCE_FEE),
            SW_PK to parsedTransaction.getString(SW_PK),
            SW_VOTING_THRESHOLD to parsedTransaction.getInt(SW_VOTING_THRESHOLD),
            SW_TRUSTCHAIN_PKS to newTrustchainPks,
            SW_BITCOIN_PKS to newBitcoinPks
        )
        val message = JSONObject(transaction).toString()

        for (swParticipantPk in oldTrustchainPks) {
            trustchain.createProposalBlock(message, swParticipantPk.hexToBytes(), SHARED_WALLET_BLOCK)
        }

        // TODO: TIMEOUT, wait for votes, collect key parts
    }

    private fun fetchSharedWalletBlocks(): List<TrustChainBlock> {
        return getTrustChainCommunity().database.getBlocksWithType(SHARED_WALLET_BLOCK)
    }

    private fun fetchLatestSharedWalletBlock(block: TrustChainBlock): TrustChainBlock? {
        return getTrustChainCommunity().database.getAllLinked(block).maxBy { it.timestamp }
    }

    /**
     * Discover joinable shared wallets, return the latest (known) blocks
     */
    public fun discoverSharedWallets(): List<TrustChainBlock> {
        val sharedWalletBlocks = fetchSharedWalletBlocks()
        val discoveredBlocks = mutableListOf<TrustChainBlock>()
        val discoveredBlockIds = mutableListOf<String>()
        for (block in sharedWalletBlocks) {
            val blockData = CoinUtil.parseTransaction(block.transaction)
            if (discoveredBlockIds.contains(blockData.getString(SW_UNIQUE_ID))) {
                continue
            }

            val latestBlock = fetchLatestSharedWalletBlock(block) ?: continue
            discoveredBlocks.add(latestBlock)
            discoveredBlockIds.add(blockData.getString(SW_UNIQUE_ID))
        }
        return discoveredBlocks
    }

    public fun fetchLatestJoinedSharedWalletBlocks(): List<TrustChainBlock> {
        return discoverSharedWallets().filter {
            val blockData = CoinUtil.parseTransaction(it.transaction)
            val users = CoinUtil.parseJSONArray(blockData.getJSONArray(SW_TRUSTCHAIN_PKS))
            users.contains(myPeer.publicKey.keyToBin().toHex())
        }
    }

    public fun transferFunds(oldSwPk: ByteArray, newSwPk: ByteArray) {
        // TODO: send funds to new wallet
    }

    companion object {
        public const val SHARED_WALLET_BLOCK = "SHARED_WALLET_BLOCK"
        public const val SW_UNIQUE_ID = "SW_UNIQUE_ID"
        public const val SW_ENTRANCE_FEE = "SW_ENTRANCE_FEE"
        public const val SW_PK = "SW_PK"
        public const val SW_VOTING_THRESHOLD = "SW_VOTING_THRESHOLD"
        public const val SW_TRUSTCHAIN_PKS = "SW_TRUSTCHAIN_PKS"
        public const val SW_BITCOIN_PKS = "SW_BLOCKCHAIN_PKS"
    }
}
