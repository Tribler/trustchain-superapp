package nl.tudelft.trustchain.common.ebsi

import android.util.Log
import com.google.common.io.BaseEncoding
import com.nimbusds.jose.*
import com.nimbusds.jose.crypto.ECDHDecrypter
import com.nimbusds.jose.crypto.ECDHEncrypter
import com.nimbusds.jwt.SignedJWT
import java.text.ParseException
import com.nimbusds.jwt.JWTClaimsSet
import com.nimbusds.jose.crypto.ECDSASigner
import com.nimbusds.jose.crypto.ECDSAVerifier
import com.nimbusds.jose.jwk.Curve
import com.nimbusds.jwt.EncryptedJWT
import java.security.interfaces.ECPrivateKey
import java.security.interfaces.ECPublicKey
import java.util.*


object JWTHelper {

    val stringClaims = listOf("state", "nonce") //client_id, scope, response_type

    fun createJWT(wallet: EBSIWallet, aud: String?, payload: Map<String, Any>?, claims: Map<String, Any>?, selfIssued: Boolean = false, detached: Boolean = false): String {
        val signer: JWSSigner = ECDSASigner(wallet.privateKey, Curve.SECP256K1)
        val jwk = wallet.keyPair.jwk()
        val thumb = jwk.computeThumbprint()

        // Prepare JWT with claims set
        val claimsSet = JWTClaimsSet.Builder()

        claimsSet.issueTime(Date())
            .expirationTime(Date(Date().time + (5 * 60 * 1000)))
            .issuer(if (selfIssued) wallet.did else wallet.did).apply {
                claim("sub", BaseEncoding.base64().encode(thumb.decode()).urlSafe())
                claim("sub_did_verification_method_uri", wallet.keyAlias)
                claim("sub_jwk", jwk.toPublicJWK().toJSONObject())
            }

        if (aud != null) claimsSet.audience(aud)

        if (payload != null) {
            for (key in payload.keys) {
                when {
                    stringClaims.contains(key) -> claimsSet.claim(key, payload[key].toString())
                }
            }
        }

        if (claims != null) {
            claimsSet.claim("claims", claims)
        }

        val signedJWT = SignedJWT(
            JWSHeader.Builder(JWSAlgorithm.ES256K).keyID(wallet.keyAlias).type(JOSEObjectType.JWT).build(),
            claimsSet.build()
        )

        // Compute the EC signature
        signedJWT.sign(signer)

        verifyJWT(signedJWT.serialize(), VerificationListener {
            Log.e("SignJWT", "Signature verified: ${it != null}")
        }, wallet.publicKey as ECPublicKey)

        // Serialize the JWS to compact form
        return signedJWT.serialize(detached)
    }

    fun verifyJWT(jwt: String, onVerified: VerificationListener, verificationKey: ECPublicKey? = null) {
        try {
            val parsedJWT = SignedJWT.parse(jwt)
            val payload = parsedJWT.payload.toJSONObject()
            val header = parsedJWT.header
            val kid = header.keyID

            /*header.toJSONObject().forEach {
                Log.e("JWT Verify", "[header] ${it.key}: ${it.value}")
            }
            payload?.forEach {
              Log.e("JWT Verify", "[payload] ${it.key}: ${it.value}")
            }*/

            if (verificationKey != null) {
                val verifier: JWSVerifier = ECDSAVerifier(verificationKey)
                // assertTrue(signedJWT.verify(verifier))
                if (parsedJWT.verify(verifier)) {
                    onVerified(payload)
                    Log.e("JWT Verify", "Verified")
                }
                return
            }

            EBSIRequest.get(null,null,
                {
                    val publicKeys = it.getJSONArray("publicKeys")
                    for (i in 0 until publicKeys.length()) {
                        val publicKeyStr = publicKeys.getString(i)
                        val publicKey = KeyStoreHelper.decodePemPublicKey(publicKeyStr)
                        val verifier: JWSVerifier = ECDSAVerifier(publicKey)
                        // assertTrue(signedJWT.verify(verifier))
                        if (parsedJWT.verify(verifier)) {
                            onVerified(payload)
                            Log.e("JWT Verify", "Verified")
                            return@get
                        }
                    }

                    onVerified(null)
                }, {
                    Log.e("JWT Verify", "Failed to get kid", it)
                    onVerified(null)
                }, redirect = kid)

        } catch (e: ParseException) {
            // Invalid signed JOSE object encoding
            Log.e("JWT Verify","Invalid JWT", e)
            onVerified(null)
        }
    }

    fun encrypt(publicKey: ECPublicKey, encJWT: EncryptedJWT): EncryptedJWT {
        val encrypter: JWEEncrypter = ECDHEncrypter(publicKey)
        encJWT.encrypt(encrypter)
        return encJWT
    }

    fun decrypt(privateKey: ECPrivateKey, encJWT: EncryptedJWT): EncryptedJWT {
        val decrypter: JWEDecrypter = ECDHDecrypter(privateKey)
        encJWT.decrypt(decrypter)
        return encJWT
    }
}

class VerificationListener(private val listener: (MutableMap<String, Any>?) -> Unit): (MutableMap<String, Any>?) -> Unit {

    override fun invoke(payload: MutableMap<String, Any>?) {
        listener(payload)
    }
}

fun String.urlSafe(): String {
    return this.replace("=", "").replace("+", "-").replace("/", "_")
}
