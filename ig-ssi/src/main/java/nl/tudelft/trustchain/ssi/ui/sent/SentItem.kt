package nl.tudelft.trustchain.ssi.ui.sent

import com.mattskala.itemadapter.Item
import nl.tudelft.ipv8.attestation.communication.AttestationPresentation
import nl.tudelft.ipv8.attestation.communication.SubjectAttestationPresentation

class SentItem(val index: Int, val attestation: SubjectAttestationPresentation) : Item() {

    override fun equals(other: Any?): Boolean {
        return other is SentItem && this.index == other.index && this.attestation == other.attestation
    }

    override fun areItemsTheSame(other: Item): Boolean {
        return equals(other)
    }

    override fun areContentsTheSame(other: Item): Boolean {
        return areItemsTheSame(other)
    }
}
