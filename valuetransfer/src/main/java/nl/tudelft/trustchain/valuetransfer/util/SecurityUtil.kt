package nl.tudelft.trustchain.valuetransfer.util

import android.os.Build
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import androidx.annotation.RequiresApi
import java.lang.RuntimeException
import java.math.BigInteger
import java.net.URLDecoder
import java.net.URLEncoder
import java.security.*
import java.security.interfaces.RSAPublicKey
import java.security.spec.RSAPublicKeySpec
import javax.security.auth.x500.X500Principal

object SecurityUtil {

    private const val KEY_ALIAS = "nl.tudelft.trustchain.key"
    private const val ANDROID_KEYSTORE = "AndroidKeyStore"

    @RequiresApi(Build.VERSION_CODES.M)
    fun generateKey(): KeyPair {
        // We are creating a RSA key pair
        val keyPairGenerator: KeyPairGenerator = KeyPairGenerator.getInstance(KeyProperties.KEY_ALGORITHM_RSA, ANDROID_KEYSTORE)
        // We are creating the key pair with sign and verify purposes
        val parameterSpec: KeyGenParameterSpec = KeyGenParameterSpec.Builder(
            KEY_ALIAS,
            KeyProperties.PURPOSE_SIGN or KeyProperties.PURPOSE_VERIFY
        ).run {
            setCertificateSerialNumber(BigInteger.valueOf(777)) // Serial number used for the self-signed certificate of the generated key pair, default is 1
            setCertificateSubject(X500Principal("CN=$KEY_ALIAS")) // Subject used for the self-signed certificate of the generated key pair, default is CN=fake
            setDigests(KeyProperties.DIGEST_SHA256) // Set of digests algorithms with which the key can be used
            setSignaturePaddings(KeyProperties.SIGNATURE_PADDING_RSA_PKCS1) // Set of padding schemes with which the key can be used when signing/verifying
            build()
        }

        // Initialization of key generator with the parameters we have specified above
        keyPairGenerator.initialize(parameterSpec)

        // Generates the key pair
        return keyPairGenerator.genKeyPair()
    }

    fun serializePK(publicKey: RSAPublicKey): String {
        return publicKey.modulus.toString() + "||" + publicKey.publicExponent.toString()
    }

    fun deserializePK(publicKey: String?): PublicKey? {
        return try {
            val split = publicKey.toString().split("||")
            val spec = RSAPublicKeySpec(BigInteger(split[0]), BigInteger(split[1]))
            KeyFactory.getInstance(KeyProperties.KEY_ALGORITHM_RSA).generatePublic(spec)
        } catch (ex: Exception) {
            null
        }
    }

    fun sign(input: String, privateKey: PrivateKey): String {
        try {
            // We sign the data with the private key. We use RSA algorithm along SHA-256 digest algorithm
            val signature: ByteArray? = Signature.getInstance("SHA256withRSA").run {
                initSign(privateKey)
                update(input.toByteArray())
                sign()
            }
            if (signature != null) {
                // We encode and store in a variable the value of the signature
                return android.util.Base64.encodeToString(signature, android.util.Base64.DEFAULT)
            }
            return ""
        } catch (e: Exception) {
            throw RuntimeException(e)
        }
    }

    fun validate(input: String, signature: String?, publicKey: PublicKey?): Boolean {
        val signatureResult: ByteArray = android.util.Base64.decode(signature, android.util.Base64.DEFAULT)
        // We check if the signature is valid. We use RSA algorithm along SHA-256 digest algorithm
        val isValid: Boolean = Signature.getInstance("SHA256withRSA").run {
            initVerify(publicKey)
            update(input.toByteArray())
            verify(signatureResult)
        }
        return isValid
    }

    fun urlencode(input: String?): String {
        return URLEncoder.encode(input, "utf-8")
    }

    fun urldecode(input: String?): String {
        return URLDecoder.decode(input, "utf-8")
    }
}
