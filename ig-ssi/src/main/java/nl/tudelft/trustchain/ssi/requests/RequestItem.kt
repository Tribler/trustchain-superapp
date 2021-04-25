package nl.tudelft.trustchain.ssi.requests

import com.mattskala.itemadapter.Item
import nl.tudelft.ipv8.Peer
import nl.tudelft.ipv8.attestation.PrivateAttestationBlob

// RecyclerView apparently does not allow inheritance, hence awkward manual type definitions.
class RequestItem(
    val index: Int,
    val requestType: Int,
    val peer: Peer,
    val attributeName: String,
    val metadata: String? = null
) : Item() {
    override fun areItemsTheSame(other: Item): Boolean {
        return other is RequestItem && this.requestType == other.requestType && this.peer.mid == other.peer.mid && this.attributeName == other.attributeName && this.metadata == other.metadata
    }

    override fun areContentsTheSame(other: Item): Boolean {
        return false
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
