package nl.tudelft.trustchain.valuetransfer.ui.identity

import com.mattskala.itemadapter.Item
import nl.tudelft.ipv8.attestation.wallet.store.AttestationBlob // OK

data class AttestationItem(
    val attestationBlob: AttestationBlob
) : Item() {
    override fun areItemsTheSame(other: Item): Boolean {
        return other is AttestationItem && this.attestationBlob.attestationHash.contentEquals(other.attestationBlob.attestationHash)
    }
}
