package nl.tudelft.trustchain.datavault.accesscontrol

import android.util.Log
import nl.tudelft.ipv8.Peer
import nl.tudelft.ipv8.attestation.wallet.AttestationBlob
import nl.tudelft.ipv8.attestation.wallet.AttestationCommunity
import java.io.File
import java.io.FileOutputStream

class AccessPolicy(
    private val file: File,
    private val attestationCommunity: AttestationCommunity
    ) {
    fun verifyAccess(peer: Peer, accessToken: String?, attestations: List<AttestationBlob>?) : Boolean {
        if (isTokenRequired()) {
            if (peer.intro){
                //REMOVE
            }
            return accessToken != null || (attestations != null && !attestations.isEmpty())

                // TEMP attestation bypass
                /*attestations?.find {
                it.metadata != null &&
                    it.signature != null &&
                    it.attestorKey != null &&
                    attestationCommunity.verifyAttestationLocally(peer, it.attestationHash, it.metadata!!, it.signature!!, it.attestorKey!!) } != null*/
        }
        return true
    }

    fun isTokenRequired(): Boolean {
        var acl = File(file.absolutePath + ".acl")
        if (acl.exists()) {
            var aclContent = acl.readText()
            if (aclContent == TRUE) {
                return true
            }
        }
        return false
    }

    fun setTokenRequired(isRequired: Boolean) {
        var acl = File(file.absolutePath + ".acl")
        var fos = FileOutputStream (acl)
        if (isRequired) {
            fos.write(TRUE.toByteArray())
        } else {
            fos.write(FALSE.toByteArray())
        }
        fos.close()
    }

    companion object {
        const val TRUE = "TRUE"
        const val FALSE = "FALSE"
    }
}
