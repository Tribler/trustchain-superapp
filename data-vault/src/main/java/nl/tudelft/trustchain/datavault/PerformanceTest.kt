package nl.tudelft.trustchain.datavault

import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import nl.tudelft.ipv8.Peer
import nl.tudelft.ipv8.attestation.schema.*
import nl.tudelft.ipv8.attestation.wallet.AttestationCommunity
import nl.tudelft.ipv8.keyvault.PrivateKey
import nl.tudelft.ipv8.util.ByteArrayKey
import nl.tudelft.ipv8.util.defaultEncodingUtils
import nl.tudelft.ipv8.util.sha1
import nl.tudelft.ipv8.util.toHex
import nl.tudelft.trustchain.common.util.TimingUtils
import nl.tudelft.trustchain.datavault.accesscontrol.AccessControlList
import nl.tudelft.trustchain.datavault.accesscontrol.Policy
import nl.tudelft.trustchain.datavault.community.DataVaultCommunity
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.IvParameterSpec
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

    fun measureEncDecryption(encrypt: Boolean) {
        val testType = if (encrypt) "encryption" else "decryption"

        val testDir = File(
            dataVaultCommunity.VAULT,
            "PERFORMANCE_TEST"
        )

        val CBC = "AES/CBC/PKCS5PADDING"
        val CTR = "AES/CTR/NoPadding"

        CoroutineScope(Dispatchers.IO).launch {
            Log.e(logTag, "Measuring CBC vs CTR $testType")

            val keygen = KeyGenerator.getInstance("AES")
            keygen.init(256)
            val secretKey = keygen.generateKey()

            val CBCResults = JSONArray()
            val CTRResults = JSONArray()

            for (r in 1..100) {
                Log.e(logTag, "${r*10}MB round")

                var duration = if (encrypt) {
                    runEncryption(CBC, secretKey, r, testDir)
                } else {
                    runDecryption(CBC, secretKey, r, testDir)
                }

                CBCResults.put(duration)

                duration = if (encrypt) {
                    runEncryption(CTR, secretKey, r, testDir)
                } else {
                    runDecryption(CTR, secretKey, r, testDir)
                }

                CTRResults.put(duration)
            }

            Log.e(logTag, "CBC $testType results: $CBCResults")
            Log.e(logTag, "CTR $testType results: $CTRResults")
        }
    }

    private fun runEncryption(bc_mode: String, secretKey: SecretKey, reps: Int, testDir: File, rounds: Int = 5): Long {
        val cipher= Cipher.getInstance(bc_mode)
        cipher.init(Cipher.ENCRYPT_MODE, secretKey, SecureRandom())

        var total = 0L

        for (r in 0..rounds) {
            val startTs = TimingUtils.getTimestamp()
            val it = File(testDir, "10MB.txt")

            for (i in 0..reps) {
                cipher.doFinal(it.readBytes())
            }
            total += TimingUtils.getTimestamp() - startTs
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
            val startTs = TimingUtils.getTimestamp()
            val it = File(testDir, filename)

            for (i in 0..reps) {
                cipher.doFinal(it.readBytes())
            }
            total += TimingUtils.getTimestamp() - startTs
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
        dataVaultCommunity.sendTestFileRequest(peer, TimingUtils.getTimestamp().toString(), listOf(filename), Policy.AccessTokenType.SESSION_TOKEN, listOf("session-token"))
    }

    fun testFileRequestTCID(peer: Peer, attestationCommunity: AttestationCommunity, filename: String) {
//        val nonce = SecureRandom.getSeed(16).toString()

        val attestations = attestationCommunity.database.getAllAttestations()
            .filter { attestationBlob -> attestationBlob.signature != null }.map {
                it.serialize().toHex()
            }

        dataVaultCommunity.sendTestFileRequest(peer, TimingUtils.getTimestamp().toString(), listOf(filename), Policy.AccessTokenType.TCID, attestations)
    }

    fun testFileRequestJWT(peer: Peer, filename: String, jwt: String) {
//        val nonce = SecureRandom.getSeed(16).toString()
        dataVaultCommunity.sendTestFileRequest(peer, TimingUtils.getTimestamp().toString(), listOf(filename), Policy.AccessTokenType.JWT, listOf(jwt))
    }

    fun addTestAttestation(attestationCommunity: AttestationCommunity) {
        val peer = dataVaultCommunity.myPeer
        val privateKey = peer.identityPrivateKeySmall!!
        val publicKey = privateKey.publicKey()
//        val metadata = hashMapOf("id_format" to ID_METADATA)

        val inputMetadata = JSONObject().apply { put("id_format", ID_METADATA) }
        val idFormat = inputMetadata.optString("id_format", "id_metadata")
        val attribute = "name"



        val metadata = JSONObject()
        metadata.put("attribute", attribute)
        // Encode to UTF-8
        metadata.put("public_key", defaultEncodingUtils.encodeBase64ToString(publicKey.serialize()))
        metadata.putOpt("id_format", idFormat)
        metadata.putOpt("signature", true)

        inputMetadata.keys().forEach {
            metadata.put(it, inputMetadata.get(it))
        }

//        val attribute = metadata.getString("attribute")
        var value = "Sharif".toByteArray() // metadata.optString("value").toByteArray()
//        val pubkeyEncoded = metadata.getString("public_key")

//        val idFormat = metadata.getString("id_format")
        val idAlgorithm = attestationCommunity.getIdAlgorithm(idFormat)
//        val shouldSign = metadata.optBoolean("signature", false)

        val stringifiedValue = when (idFormat) {
            ID_METADATA_RANGE_18PLUS -> ID_METADATA_RANGE_18PLUS_PUBLIC_VALUE
            ID_METADATA_RANGE_UNDERAGE -> ID_METADATA_RANGE_UNDERAGE_PUBLIC_VALUE
            else -> String(value)
        }

        metadata.put("value", stringifiedValue)
        metadata.put("trustchain_address_hash", peer.publicKey.keyToHash().toHex())

        // Decode as UTF-8 ByteArray
//        val publicKey = idAlgorithm.loadPublicKey(defaultEncodingUtils.decodeBase64FromString(pubkeyEncoded))
        val attestationBlob = idAlgorithm.attest(publicKey, value)
        val attestation =
            idAlgorithm.deserialize(attestationBlob, idFormat)

        val signableData = attestation.getHash() + metadata.toString().toByteArray()
        val signature = (peer.key as PrivateKey).sign(sha1(signableData))

        attestationCommunity.attestationKeys[ByteArrayKey(attestation.getHash())] = Pair(privateKey, idFormat)
        attestationCommunity.database.insertAttestation(attestation,
            attestation.getHash(),
            privateKey,
            idFormat,
            metadata.toString(),
            signature,
            peer.publicKey)

        Log.e("PerfTest", "Successfully added attestation")
    }

    fun testTCID(attestationCommunity: AttestationCommunity) {
        val attestations = attestationCommunity.database.getAllAttestations()

        Log.e("PerfTest", "Attestations: ${attestations.size}")

        val start = TimingUtils.getTimestamp()
        val verified = AccessControlList(dataVaultCommunity.VAULT, dataVaultCommunity, attestationCommunity).
        filterAttestations(dataVaultCommunity.myPeer, attestations)?.size ?: 0 > 0
        val duration = TimingUtils.getTimestamp() - start
        Log.e("PerfTest", "$duration ms to verify ($verified) ${attestations.size} attestations")
    }

    companion object {
        const val logTag = "PerfTest"
    }
}
