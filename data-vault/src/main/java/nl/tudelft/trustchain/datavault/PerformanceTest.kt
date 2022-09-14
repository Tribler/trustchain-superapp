package nl.tudelft.trustchain.datavault

import android.util.Log
import id.walt.crypto.toHexString
import nl.tudelft.ipv8.Peer
import nl.tudelft.ipv8.attestation.wallet.AttestationCommunity
import nl.tudelft.trustchain.datavault.accesscontrol.Policy
import nl.tudelft.trustchain.datavault.community.DataVaultCommunity
import java.security.SecureRandom
import java.time.Instant
import java.util.*

class PerformanceTest(
    private val peer: Peer,
    private val dataVaultCommunity: DataVaultCommunity,
    private val attestationCommunity: AttestationCommunity
) {
/*
        Test different verification scenarios:
        1. session token
        2. TCID
        3. EBSI JWT
        4. EBSI JSONLD
*/

    fun run() {
        val filename = "PERFORMANCE_TEST/1.jpg"
        testFileRequestSessionToken(filename)
        testFileRequestTCID(filename)
    }

    fun getTestFiles() {
//        val testFiles = mutableListOf<String>()
    }

    fun testFileRequestSessionToken(filename: String) {
//        val nonce = SecureRandom.getSeed(16).toString()
        dataVaultCommunity.sendTestFileRequest(peer, getTimestamp(), filename,Policy.AccessTokenType.SESSION_TOKEN, listOf("session-token"))
    }

    fun testFileRequestTCID(filename: String) {
//        val nonce = SecureRandom.getSeed(16).toString()

        val attestations = attestationCommunity.database.getAllAttestations()
            .filter { attestationBlob -> attestationBlob.signature != null }.map {
                it.serialize().toHexString()
            }

        dataVaultCommunity.sendTestFileRequest(peer, getTimestamp(), filename,Policy.AccessTokenType.TCID, attestations)
    }

    fun testFileRequestJWT(filename: String, jwt: String) {
//        val nonce = SecureRandom.getSeed(16).toString()
        dataVaultCommunity.sendTestFileRequest(peer, getTimestamp(), filename,Policy.AccessTokenType.JWT, listOf(jwt))
    }

    companion object {
        fun getTimestamp(): String{
            return Instant.now().toEpochMilli().toString()
        }
    }
}
