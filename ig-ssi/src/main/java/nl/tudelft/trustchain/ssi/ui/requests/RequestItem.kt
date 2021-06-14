package nl.tudelft.trustchain.ssi.ui.requests

import com.mattskala.itemadapter.Item
import nl.tudelft.ipv8.Peer

// RecyclerView apparently does not allow inheritance, hence awkward manual type definitions.
class RequestItem(
    val index: Int,
    val requestType: Int,
    val peer: Peer,
    val attributeName: String,
    val metadata: String? = null,
    val requestedValue: String? = null,
) : Item() {
    override fun equals(other: Any?): Boolean {
        return other is RequestItem && this.index == other.index && this.requestType == other.requestType && this.peer.mid == other.peer.mid && this.attributeName == other.attributeName && this.metadata == other.metadata && this.requestedValue == other.requestedValue
    }

    override fun areItemsTheSame(other: Item): Boolean {
        return this == other
    }

    override fun areContentsTheSame(other: Item): Boolean {
        return this == other
    }

    fun isVerifyRequest(): Boolean {
        return this.requestType == VERIFY_REQUEST_ITEM
    }

    fun isAttestationRequest(): Boolean {
        return this.requestType == ATTESTATION_REQUEST_ITEM
    }

    companion object {
        const val ATTESTATION_REQUEST_ITEM = 0
        const val VERIFY_REQUEST_ITEM = 1
    }
}
