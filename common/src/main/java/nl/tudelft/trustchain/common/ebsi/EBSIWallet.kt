package nl.tudelft.trustchain.common.ebsi

import android.content.Context
import android.security.keystore.KeyProperties
import android.util.Log
import io.ipfs.multibase.Multibase
import nl.tudelft.ipv8.util.sha512
import nl.tudelft.ipv8.util.toHex
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.io.File
import java.security.KeyPair
import java.security.KeyPairGenerator
import java.security.Signature
import java.security.interfaces.ECPrivateKey
import java.security.interfaces.ECPublicKey
import java.security.spec.ECGenParameterSpec
import javax.crypto.Cipher
import javax.crypto.KeyAgreement
import javax.crypto.SecretKey
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec
import kotlin.random.Random

class EBSIWallet(
    private val context: Context
) {

    private val keyStoreHelper = KeyStoreHelper(this)

    val ebsiWalletDir by lazy { File(context.filesDir, EBSI_WALLET_DIR).also { it.mkdir() } }
    private val didFile by lazy { File(ebsiWalletDir, EBSI_DID_FILE) }
    private val vaFile by lazy { File(ebsiWalletDir, EBSI_VA_FILE) }

    private lateinit var didCache: String
    private lateinit var keyPairCache: KeyPair
    val privateKey: ECPrivateKey = keyPair.private as ECPrivateKey
    val publicKey: ECPublicKey = keyPair.public as ECPublicKey
    val keyAlias = "$did#keys-1"
    var accessToken: Map<String, Any>? = null
    private var vaCache: JSONObject? = null

    val did: String get() {
        if (!this::didCache.isInitialized) {
            didCache = if (didFile.exists()){
                didFile.readText()
            } else {
                createDid()
            }
        }
        return didCache
    }

    val keyPair: KeyPair get() {
        if (!this::keyPairCache.isInitialized) {
            val kPair = keyStoreHelper.getKeyPair()
            keyPairCache = kPair ?: newKeyPair()
        }
        return keyPairCache
    }

    private fun newKeyPair(store: Boolean = false): KeyPair {
        Log.e(TAG, "Creating new key pair")
        val kpg: KeyPairGenerator = KeyPairGenerator.getInstance(
            KeyProperties.KEY_ALGORITHM_EC,
            KEYSTORE_PROVIDER
        )
        val parameterSpec = ECGenParameterSpec("secp256k1")
        kpg.initialize(parameterSpec)
        val keyPair = kpg.generateKeyPair()
        if (store) keyStoreHelper.storeKey(keyPair)
        return keyPair
    }

    fun createDid(): String {
        // Check with /did-registry/v2/identifiers/$did id did exists
        val seed = Random.nextBytes(16)

        val data = appendBytesToVersion(seed)
        val id = Multibase.encode(Multibase.Base.Base58BTC, data)
        val did = "did:ebsi:$id"
        didFile.createNewFile()
        didFile.writeText(did)
        return did
    }

    fun didDocument(): JSONObject {
        val document = JSONObject()

        val verificationMethod = JSONObject().apply {
            put("id", keyAlias)
            put("type", "Secp256k1VerificationKey2018")
            put("controller", did)
            put("publicKeyHex", publicKey.encoded.toHex())
        }

        document.apply {
            put("@context", JSONArray().put("https://www.w3.org/ns/did/v1"))
            put("id", did)
            put("verificationMethod", JSONArray().put(verificationMethod))
            put("authentication", JSONArray().put(keyAlias))
            put("assertionMethod", JSONArray().put(keyAlias))
        }

        return document
    }

    val verifiableAuthorisation: JSONObject? get() {
        if (vaCache == null) {
            if (vaFile.exists()){
                vaCache = JSONObject(vaFile.readText())
            }
        }
        return vaCache
    }

    fun storeVerifiableAuthorisation(verifiableAuthorisation: JSONObject) {
        vaFile.createNewFile()
        vaFile.writeText(verifiableAuthorisation.toString())
    }

    fun getAccessToken(accessTokenRequest: JSONObject, accessTokenCallback: (payload: MutableMap<String, Any>?) -> Unit) {
        val encPayload = accessTokenRequest.getString("ake1_enc_payload")
        val eciesPayload = ECIESPayload.parseCiphertext(encPayload)

        val decryptedPayload = decrypt(eciesPayload).toString(Charsets.UTF_8)
        val payload = JSONObject(decryptedPayload)
        val accessTokenJWT = payload.getString("access_token")
        Log.e("Ake1", "Decrypted payload: ${payload.toString()}")

        // TODO something with ake1_sig_payload and ake1_jws_detached

        JWTHelper.verifyJWT(accessTokenJWT) { JWTPayload ->
            // TODO store accessToken for 15 min
            accessToken = JWTPayload
            accessTokenCallback(JWTPayload)
        }
    }

    fun sign(data: ByteArray): ByteArray {
        return Signature.getInstance("SHA256withECDSA").run {
            initSign(privateKey)
            update(data)
            sign()
        }
    }

    fun verify(data: ByteArray, signature: ByteArray): Boolean {
        return Signature.getInstance("SHA256withECDSA").run {
            initVerify(publicKey)
            update(data)
            verify(signature)
        }
    }

    fun encrypt(data: ByteArray): ByteArray {
        // TODO
        return data
    }

    fun decrypt(eciesPayload: ECIESPayload): ByteArray {
        val keyAgreement = KeyAgreement.getInstance("ECDH").apply {
            init(privateKey)
            doPhase(KeyStoreHelper.loadPublicKey(eciesPayload.ephemPublickKey.pubKey), true)
        }

        val derivation = sha512(keyAgreement.generateSecret())
        val sessionKey = derivation.copyOfRange(0, 32)

        // TODO verify hmac
        // macKey = derivation[32-64]
        // data to mac = iv + ephemKey + ciphertext
        // hmacSha256Verify(macKey, dataToMac, mac)

        val secretKey: SecretKey = SecretKeySpec(sessionKey, 0, sessionKey.size, "AES")

        return Cipher.getInstance("AES/CBC/PKCS5Padding").run {
            val ivSpec = IvParameterSpec(eciesPayload.iv)
            init(Cipher.DECRYPT_MODE, secretKey, ivSpec)
            doFinal(eciesPayload.ciphertext)
        }
    }

    companion object {
        private const val TAG = "EBSI Wallet"
        private const val KEYSTORE_PROVIDER = "SC"

        const val EBSI_WALLET_DIR = "EBSI_WALLET"
        const val EBSI_DID_FILE = "EBSI_DID"
        const val EBSI_VA_FILE = "EBSI_VA"

        private fun appendBytesToVersion(arr: ByteArray, version: Int = 1): ByteArray {
            val baos = ByteArrayOutputStream()
            baos.write(version)
            baos.write(arr)
            return baos.toByteArray()
        }
    }
}
