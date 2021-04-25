package nl.tudelft.trustchain.ssi.database

import com.mattskala.itemadapter.Item
import nl.tudelft.ipv8.attestation.PrivateAttestationBlob

class DatabaseItem(val index: Int, val attestation: PrivateAttestationBlob) : Item() {
    override fun areItemsTheSame(other: Item): Boolean {
        return other is DatabaseItem && this.attestation.attributeHash.contentEquals(other.attestation.attributeHash)
    }

    override fun areContentsTheSame(other: Item): Boolean {
        return false
    }
}
