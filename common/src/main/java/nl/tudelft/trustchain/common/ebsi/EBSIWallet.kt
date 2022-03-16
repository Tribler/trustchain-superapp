package nl.tudelft.trustchain.common.ebsi

import android.content.Context
import android.security.keystore.KeyProperties
import android.util.Log
import io.ipfs.multibase.Multibase
import nl.tudelft.ipv8.util.toHex
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.io.File
import java.security.*
import java.security.spec.ECGenParameterSpec
import kotlin.random.Random

class EBSIWallet(
    private val context: Context
) {

    companion object {
        private const val TAG = "EBSI Wallet"
        private const val KEYSTORE_PROVIDER = "SC"

        const val EBSI_WALLET_DIR = "EBSI_WALLET"
        const val EBSI_DID_FILE = "EBSI_DID"
    }

    private val keyStoreHelper = KeyStoreHelper(this)

    val ebsiWalletDir by lazy { File(context.filesDir, EBSI_WALLET_DIR).also { it.mkdir() } }
    private val didFile by lazy { File(ebsiWalletDir, EBSI_DID_FILE) }

    private lateinit var didCache: String

    val did: String get() {
        if (!this::didCache.isInitialized) {
            val didFile = File(ebsiWalletDir, EBSI_DID_FILE)

            didCache = if (didFile.exists() && false){
                didFile.readText()
            } else {
                createDid()
            }
        }
        return didCache
    }

    private lateinit var keyPairCache: KeyPair

    val privateKey: PrivateKey = keyPair.private
    val publicKey: PublicKey = keyPair.public
    val keyAlias = "$did#keys-1"

    val keyPair: KeyPair get() {
        if (!this::keyPairCache.isInitialized) {
            val kPair = keyStoreHelper.getKeyPair()
            keyPairCache = kPair ?: newKeyPair()
        }
        keyPairCache.public
        Log.e(TAG, "Key Pair initialized: ${keyPairCache.private.encoded.toHex()} | ${keyPairCache.public.encoded.toHex()}")
        return keyPairCache
    }

    private fun newKeyPair(): KeyPair {
        /*val ecJWK: ECKey = ECKeyGenerator(Curve.P_256)
            .keyID("123")
            .generate()
        val ecPublicJWK: ECKey = ecJWK.toPublicJWK()*/

        Log.e(TAG, "Creating new key pair")
        val kpg: KeyPairGenerator = KeyPairGenerator.getInstance(
            KeyProperties.KEY_ALGORITHM_EC,
            KEYSTORE_PROVIDER
        )
        val parameterSpec = ECGenParameterSpec("secp256k1")

        kpg.initialize(parameterSpec)
        val keyPair = kpg.generateKeyPair()
        keyStoreHelper.storeKey(keyPair)
        return keyPair
    }

    private fun appendBytesToVersion(i: Int, arr: ByteArray): ByteArray {
        val baos = ByteArrayOutputStream()
        baos.write(i)
        baos.write(arr)
        return baos.toByteArray()
    }

    fun createDid(): String {
        // Check with /did-registry/v2/identifiers/$did id did exists
        val seed = Random.nextBytes(16)

        val data = appendBytesToVersion(1, seed)
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
}
