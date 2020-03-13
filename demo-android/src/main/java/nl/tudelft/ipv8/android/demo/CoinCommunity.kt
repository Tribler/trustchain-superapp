package nl.tudelft.ipv8.android.demo
import android.util.JsonWriter
import android.util.Log
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

    public fun sendCurrency(amount: Double, toPublicKey: ByteArray = myPeer.publicKey.keyToBin()) {
        val message = "Transaction amount: $amount bitcoins"
        trustchain.createProposalBlock(message, toPublicKey, SW_JOIN_BLOCK)
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
            SW_ENTRANCE_FEE to entranceFee,
            SW_PK to sharedWalletPK.toHex(),
            SW_VOTING_THRESHOLD to votingThreshold,
            SW_TRUSTCHAIN_PKS to arrayListOf(trustchainPk.toHex()),
            SW_BITCOIN_PKS to arrayListOf(bitcoinPk.toHex())
        )
        val values = JSONObject(transactionValues).toString()
        trustchain.createProposalBlock(values, trustchainPk, SW_JOIN_BLOCK)
    }

    public fun discoverSharedWalletsTrustchainPublicKeys(): List<ByteArray> {
        val sharedWalletBlocks = getTrustChainCommunity().database.getBlocksWithType(SW_JOIN_BLOCK)
        val wallets = mutableListOf<ByteArray>()
        for (block in sharedWalletBlocks) {
            val publicKey = block.publicKey
            // TODO: Filter on the Shared Bitcoin Wallet PK's (currently always the same)
            if (wallets.none { byteArray -> byteArray.toHex() == publicKey.toHex() }) {
                wallets.add(publicKey)
            }
        }
        return wallets
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
            SW_ENTRANCE_FEE to parsedTransaction.getDouble(SW_ENTRANCE_FEE),
            SW_PK to parsedTransaction.getString(SW_PK),
            SW_VOTING_THRESHOLD to parsedTransaction.getInt(SW_VOTING_THRESHOLD),
            SW_TRUSTCHAIN_PKS to newTrustchainPks,
            SW_BITCOIN_PKS to newBitcoinPks
        )
        val message = JSONObject(transaction).toString()

        for (swParticipantPk in oldTrustchainPks) {
            Log.i("Coin", "Fetched= ${swParticipantPk.hexToBytes().toHex()}")
            Log.i("Coin", "Should be= ${myPeer.publicKey.keyToBin().toHex()}")
            trustchain.createProposalBlock(message, swParticipantPk.hexToBytes(), SW_JOIN_AGREEMENT_BLOCK)
        }

        // TODO: TIMEOUT, wait for votes, collect key parts
    }

    public fun transferFunds(oldSwPk: ByteArray, newSwPk: ByteArray) {
        // TODO: send funds to new wallet
    }

    companion object {
        public const val SW_JOIN_BLOCK = "SW_JOIN"
        public const val SW_JOIN_AGREEMENT_BLOCK = "SW_JOIN"
        public const val SW_ENTRANCE_FEE = "SW_ENTRANCE_FEE"
        public const val SW_PK = "SW_PK"
        public const val SW_VOTING_THRESHOLD = "SW_VOTING_THRESHOLD"
        public const val SW_TRUSTCHAIN_PKS = "SW_TRUSTCHAIN_PKS"
        public const val SW_BITCOIN_PKS = "SW_BLOCKCHAIN_PKS"
    }
}
