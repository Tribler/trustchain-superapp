package nl.tudelft.trustchain.ssi.ui.database

import com.mattskala.itemadapter.Item
import nl.tudelft.ipv8.attestation.communication.AttestationPresentation // OK

class DatabaseItem(val index: Int, val attestation: AttestationPresentation) : Item() {

    override fun equals(other: Any?): Boolean {
        return other is DatabaseItem && this.index == other.index && this.attestation == other.attestation
    }

    override fun areItemsTheSame(other: Item): Boolean {
        return this == other
    }

    override fun areContentsTheSame(other: Item): Boolean {
        return this == other
    }

    override fun hashCode(): Int {
        var result = index
        result = 31 * result + attestation.hashCode()
        return result
    }
}
