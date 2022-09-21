package nl.tudelft.trustchain.common.ebsi

import android.util.Log
import com.google.common.io.BaseEncoding
import com.nimbusds.jose.*
import com.nimbusds.jose.crypto.ECDHDecrypter
import com.nimbusds.jose.crypto.ECDHEncrypter
import com.nimbusds.jose.crypto.impl.ECDSA
import com.nimbusds.jose.util.Base64URL
import com.nimbusds.jwt.EncryptedJWT
import com.nimbusds.jwt.JWTClaimsSet
import com.nimbusds.jwt.SignedJWT
import id.walt.crypto.Key
import id.walt.crypto.KeyAlgorithm
import nl.tudelft.ipv8.util.toHex
import java.security.PublicKey
import java.security.SecureRandom
import java.security.Signature
import java.security.interfaces.ECPrivateKey
import java.security.interfaces.ECPublicKey
import java.text.ParseException
import java.util.*


object JWTHelper {

    val stringClaims = listOf("state", "nonce") //client_id, scope, response_type

    fun createJWT(wallet: EBSIWallet, aud: String?, payload: Map<String, Any>?, claims: Map<String, Any>?, selfIssued: Boolean = false, detached: Boolean = false): String {
//        val signer: JWSSigner = ECDSASigner(wallet.privateKey, Curve.SECP256K1)
        val jwk = wallet.keyPair.jwk()
        val thumb = jwk.computeThumbprint()
//        Log.e("JWK", "priv: ${jwk.toJSONString()}")
//        Log.e("JWK", "pub: ${jwk.toPublicJWK().toJSONString()}")
        // Prepare JWT with claims set
        val claimsSet = JWTClaimsSet.Builder()

        claimsSet.issueTime(Date())
            .expirationTime(Date(Date().time + (5 * 60 * 1000)))
            .issuer(if (selfIssued) "https://self-issued.me" else wallet.did).apply {
                claim("sub", BaseEncoding.base64().encode(thumb.decode()).urlSafe())
                claim("sub_did_verification_method_uri", wallet.keyAlias)
                claim("sub_jwk", jwk.toPublicJWK().toJSONObject().also {
                    it["kid"] = wallet.keyAlias
                })
            }

        if (aud != null) claimsSet.audience(aud)

        if (payload != null) {
            for (key in payload.keys) {
                when {
                    stringClaims.contains(key) -> claimsSet.claim(key, payload[key].toString())
                }
            }
        }

        val OVERLOAD = false

        if (OVERLOAD) {
            val rand = SecureRandom.getSeed(400 * 1000).toHex()
            claimsSet.claim("rand", rand)
        }

        if (claims != null) {
            claimsSet.claim("claims", claims)
        }

        val signedJWT = SignedJWT(
            JWSHeader.Builder(JWSAlgorithm.ES256K).keyID(wallet.keyAlias).type(JOSEObjectType.JWT).build(),
            claimsSet.build()
        )

        val signatureBytes = signJWT(wallet, signedJWT)
        val serializedJWT = serializeJWT(signedJWT, signatureBytes, false)
        Log.e("CreateJWT", "serialized jwt: $serializedJWT")


        verifyJWT(serializedJWT, VerificationListener {
            Log.e("SignJWT", "Signature verified: ${it != null}")
        }, wallet.publicKey as ECPublicKey, isDer = false)

        // Compute the EC signature
//        signedJWT.sign(signer)

        return serializeJWT(signedJWT, signatureBytes, detached)
    }

    fun createSessionToken(wallet: EBSIWallet, aud: String): String {
        val claimsSet = JWTClaimsSet.Builder()

        claimsSet.issueTime(Date())
            .expirationTime(Date(Date().time + (15 * 60 * 1000)))
            .issuer(wallet.did)
            .audience(aud)
            .claim("AFT", "AFT")

        val signedJWT = SignedJWT(
            JWSHeader.Builder(JWSAlgorithm.ES256K).keyID(wallet.keyAlias).type(JOSEObjectType.JWT).build(),
            claimsSet.build()
        )

        val signatureBytes = signJWT(wallet, signedJWT)
        return serializeJWT(signedJWT, signatureBytes, false)
    }

    fun createProof(wallet: EBSIWallet, iss: String, aud: String, nonce: String): String {
        val claimsSet = JWTClaimsSet.Builder()
            .issuer(iss)
            .audience(aud)
            .issueTime(Date())
            .claim("nonce", nonce)

        val jwk = wallet.keyPair.jwk()

        val signedJWT = SignedJWT(
            JWSHeader.Builder(JWSAlgorithm.ES256K).type(JOSEObjectType.JWT).jwk(jwk.toPublicJWK()).build(),
            claimsSet.build()
        )

        val signature = signJWT(wallet, signedJWT)
        return serializeJWT(signedJWT, signature, false)
    }

    fun serializeJWT(jwt: JWSObject, signature: Base64URL, detachedPayload: Boolean): String {
        return when {
            detachedPayload -> {
                jwt.header.toBase64URL().toString() + '.' + '.' + signature.toString()
            }
            jwt.header.isBase64URLEncodePayload -> {
                jwt.header.toBase64URL().toString() + '.' + jwt.payload.toBase64URL().toString() + '.' + signature.toString()
            }
            else -> {
                jwt.header.toBase64URL().toString() + '.' + jwt.payload.toString() + '.' + signature.toString()
            }
        }
    }

    fun signJWT(wallet: EBSIWallet, signedJWT: SignedJWT): Base64URL {
        val signature: Signature = Signature.getInstance("SHA256withECDSA", "SC")
        signature.initSign(wallet.privateKey)
        signature.update(signedJWT.signingInput)
        return Base64URL.encode(
            ECDSA.transcodeSignatureToConcat(
                signature.sign(),
                ECDSA.getSignatureByteArrayLength(signedJWT.header.algorithm)
            )
        )
    }

    fun verifyJWT(jwt: String, onVerified: VerificationListener, verificationKey: PublicKey? = null, isDer: Boolean = false) {
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
//                val verifier: JWSVerifier = ECDSAVerifier(verificationKey)
//                 assertTrue(signedJWT.verify(verifier))
//                if (parsedJWT.verify(verifier)) {
                if (secp256k1Verify(verificationKey, parsedJWT, isDer)) {
                    Log.e("JWT Verify", "Verified with provided key")
                    onVerified(payload)
                } else {
                    Log.e("JWT Verify", "Not verified with provided key")
                    onVerified(null)
                }
                return
            }

            EBSIRequest.get(null,null,
                {
                    Log.e("JWT TEST", "ebsi key id: $it")
                    val publicKeys = it.getJSONArray("publicKeys")
                    for (i in 0 until publicKeys.length()) {
                        val publicKeyStr = publicKeys.getString(i)
                        val decodedKey = Base64.getDecoder().decode(publicKeyStr)

                        Log.e("JWTH TEST", "Decoded pub key: $decodedKey")

                        val publicKey = KeyStoreHelper.decodePemPublicKey(publicKeyStr)
                        Log.e("JWTH TEST", "Decoded PEM pub key: $publicKey")

//                        val verifier: JWSVerifier = ECDSAVerifier(publicKey)
                        // assertTrue(signedJWT.verify(verifier))
//                        if (parsedJWT.verify(verifier)) {
                        if (secp256k1Verify(publicKey, parsedJWT, isDer)) {
                            Log.e("JWT Verify", "Verified")
                            onVerified(payload)
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

    fun secp256k1Verify(pk: PublicKey, jwt: JWSObject, isDer: Boolean = false): Boolean {
        val jwsSignature: ByteArray = jwt.signature.decode()

        val signature: Signature = Signature.getInstance("SHA256withECDSA", "SC")
        signature.initVerify(pk)
        signature.update(jwt.signingInput)
        return signature.verify(if (isDer) jwsSignature else ECDSA.transcodeSignatureToDER(jwsSignature))
//        return signature.verify(jwsSignature)
    }

    fun getJWTPayload(jwt: String): MutableMap<String, Any> {
        val parsedJWT = SignedJWT.parse(jwt)
        return parsedJWT.payload.toJSONObject()
    }

    private fun getSignature(key: Key): Signature {
        val sig = when (key.algorithm) {
            KeyAlgorithm.EC -> Signature.getInstance("SHA256withECDSA")
            KeyAlgorithm.ECDSA_Secp256k1 -> Signature.getInstance("SHA256withECDSA")
//            KeyAlgorithm.EdDSA_Ed25519 -> Signature.getInstance("Ed25519")
            KeyAlgorithm.EdDSA_Ed25519 -> Signature.getInstance("NONEwithEdDSA")

            KeyAlgorithm.RSA -> Signature.getInstance("SHA256withRSA")
        }
        return sig
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
