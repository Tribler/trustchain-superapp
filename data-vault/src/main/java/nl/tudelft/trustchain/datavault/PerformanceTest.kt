package nl.tudelft.trustchain.datavault

import android.content.Context
import android.util.Log
import id.walt.crypto.toHexString
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import nl.tudelft.ipv8.Peer
import nl.tudelft.ipv8.attestation.wallet.AttestationCommunity
import nl.tudelft.trustchain.datavault.accesscontrol.Policy
import nl.tudelft.trustchain.datavault.community.DataVaultCommunity
import org.json.JSONObject
import java.io.File
import java.security.SecureRandom
import java.time.Instant
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.IvParameterSpec
import kotlin.math.log

class PerformanceTest(
    private val dataVaultCommunity: DataVaultCommunity
) {
/*
        Test different verification scenarios:
        1. session token
        2. TCID
        3. EBSI JWT
        4. EBSI JSONLD
*/

    fun measureEncryption() {
        val testDir = File(
            dataVaultCommunity.VAULT,
            "PERFORMANCE_TEST"
        )

        val reps = listOf(1, 10, 50, 100, 500, 1000)

        val CBC = "AES/CBC/PKCS5PADDING"
        val CTR = "AES/CTR/NoPadding"

        CoroutineScope(Dispatchers.IO).launch {
            Log.e(logTag, "Measuring CBC vs CTR encryption")

            val keygen = KeyGenerator.getInstance("AES")
            keygen.init(256)
            val secretKey = keygen.generateKey()

            val CBCResults = JSONObject()
            val CTRResults = JSONObject()

            for (r in reps) {
                Log.e(logTag, "${r*10}MB round")

//                var duration = runEncryption(CBC, secretKey, r, testDir)
                var duration = runDecryption(CBC, secretKey, r, testDir)
                CBCResults.put("${r*10}MB", duration)
                Log.e(logTag, "CBC done")

//                duration = runEncryption(CTR, secretKey, r, testDir)
                duration = runDecryption(CTR, secretKey, r, testDir)
                CTRResults.put("${r*10}MB", duration)
                Log.e(logTag, "CTR done")
            }

            Log.e(logTag, "CBC results: $CBCResults")
            Log.e(logTag, "CTR results: $CTRResults")
        }
    }

    private fun runEncryption(bc_mode: String, secretKey: SecretKey, reps: Int, testDir: File, rounds: Int = 5): Long {
        val cipher= Cipher.getInstance(bc_mode)
        cipher.init(Cipher.ENCRYPT_MODE, secretKey, SecureRandom())

        var total = 0L

        for (r in 0..rounds) {
            val startTs = getTimestamp()
            val it = File(testDir, "10MB.txt")

            for (i in 0..reps) {
                cipher.doFinal(it.readBytes())
            }
            total += getTimestamp() - startTs
        }

        return total / rounds
    }

    private fun runDecryption(bc_mode: String, secretKey: SecretKey, reps: Int, testDir: File, rounds: Int = 5): Long {
        val encCipher= Cipher.getInstance(bc_mode)
        val ivSpec = IvParameterSpec(SecureRandom.getSeed(16))
        encCipher.init(Cipher.ENCRYPT_MODE, secretKey, ivSpec, SecureRandom())

        val f = File(testDir, "10MB.txt")
        val encrypted = encCipher.doFinal(f.readBytes())

        val filename = if (bc_mode.contains("CBC")) "CBC.txt" else "CTR.txt"
        File(testDir, filename).writeBytes(encrypted)

        val cipher= Cipher.getInstance(bc_mode)
        cipher.init(Cipher.DECRYPT_MODE, secretKey, ivSpec, SecureRandom())

        var total = 0L

        for (r in 0..rounds) {
            val startTs = getTimestamp()
            val it = File(testDir, filename)

            for (i in 0..reps) {
                cipher.doFinal(it.readBytes())
            }
            total += getTimestamp() - startTs
        }

        return total / rounds
    }

    fun testFileRequests(peer: Peer, attestationCommunity: AttestationCommunity) {
        val filename = "PERFORMANCE_TEST/1.jpg"
        testFileRequestSessionToken(peer, filename)
        testFileRequestTCID(peer, attestationCommunity, filename)
//        testFileRequestJWT
    }

    fun testFileRequestSessionToken(peer: Peer, filename: String) {
//        val nonce = SecureRandom.getSeed(16).toString()
        dataVaultCommunity.sendTestFileRequest(peer, getTimestamp().toString(), filename,Policy.AccessTokenType.SESSION_TOKEN, listOf("session-token"))
    }

    fun testFileRequestTCID(peer: Peer, attestationCommunity: AttestationCommunity, filename: String) {
//        val nonce = SecureRandom.getSeed(16).toString()

        val attestations = attestationCommunity.database.getAllAttestations()
            .filter { attestationBlob -> attestationBlob.signature != null }.map {
                it.serialize().toHexString()
            }

        dataVaultCommunity.sendTestFileRequest(peer, getTimestamp().toString(), filename,Policy.AccessTokenType.TCID, attestations)
    }

    fun testFileRequestJWT(peer: Peer, filename: String, jwt: String) {
//        val nonce = SecureRandom.getSeed(16).toString()
        dataVaultCommunity.sendTestFileRequest(peer, getTimestamp().toString(), filename,Policy.AccessTokenType.JWT, listOf(jwt))
    }

    companion object {
        const val logTag = "PerfTest"

        fun getTimestamp(): Long {
            return Instant.now().toEpochMilli()
        }
    }
}
