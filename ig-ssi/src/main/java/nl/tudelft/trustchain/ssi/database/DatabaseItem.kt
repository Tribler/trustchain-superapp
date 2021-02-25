package nl.tudelft.trustchain.ssi.database

import com.mattskala.itemadapter.Item
import nl.tudelft.ipv8.attestation.wallet.AttestationBlob

class DatabaseItem(val index: Int, val attestationBlob: AttestationBlob) : Item() {
    override fun areItemsTheSame(other: Item): Boolean {
        return other is DatabaseItem && this.attestationBlob.attestationHash.contentEquals(other.attestationBlob.attestationHash)
    }

    override fun areContentsTheSame(other: Item): Boolean {
        return false
    }
}
