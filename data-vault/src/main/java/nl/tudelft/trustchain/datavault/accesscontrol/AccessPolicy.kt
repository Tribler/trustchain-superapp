package nl.tudelft.trustchain.datavault.accesscontrol

import nl.tudelft.ipv8.Peer
import nl.tudelft.ipv8.attestation.wallet.AttestationBlob
import nl.tudelft.ipv8.attestation.wallet.AttestationCommunity

class AccessPolicy(private val attestationCommunity: AttestationCommunity) {

    fun verifyAttestations(peer: Peer, attestations: List<AttestationBlob>) : Boolean {
        return attestations.find {
                it.metadata != null &&
                it.signature != null &&
                it.attestorKey != null &&
            attestationCommunity.verifyAttestationLocally(peer, it.attestationHash, it.metadata!!, it.signature!!, it.attestorKey!!) } != null
    }
}
