package nl.tudelft.ipv8.android.demo
import nl.tudelft.ipv8.Address
import nl.tudelft.ipv8.Community
import nl.tudelft.ipv8.IPv8
import nl.tudelft.ipv8.android.IPv8Android
import nl.tudelft.ipv8.attestation.trustchain.*
import nl.tudelft.ipv8.attestation.trustchain.store.TrustChainStore
import nl.tudelft.ipv8.keyvault.Key
import java.util.*

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
        trustchain.createProposalBlock(message, toPublicKey, BLOCK_TYPE)
    }

    companion object {
        private const val BLOCK_TYPE = "coin_block"
    }
}
