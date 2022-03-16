package nl.tudelft.trustchain.common.ebsi

import android.util.Log
import com.google.common.io.BaseEncoding
import com.nimbusds.jose.*
import com.nimbusds.jwt.SignedJWT
import java.text.ParseException
import com.nimbusds.jwt.JWTClaimsSet
import com.nimbusds.jose.crypto.ECDSASigner
import com.nimbusds.jose.crypto.ECDSAVerifier
import com.nimbusds.jose.jwk.Curve
import java.util.*


object JWTHelper {

    val stringClaims = listOf("state", "nonce") //client_id, scope, response_type

    fun createJWT(wallet: EBSIWallet, aud: String?, payload: Map<String, Any>?): String {
        /*val ecJWK: ECKey = ECKeyGenerator(Curve.P_256)
            .keyID("123")
            .generate()
        val ecPublicJWK: ECKey = ecJWK.toPublicJWK()*/

        val signer: JWSSigner = ECDSASigner(wallet.privateKey, Curve.SECP256K1)

        // Prepare JWT with claims set
        val claimsSet = JWTClaimsSet.Builder()
            .issueTime(Date())
            .expirationTime(Date(Date().time + (5 * 60 * 1000)))
            .issuer(wallet.did)

        if (aud != null) claimsSet.audience(aud)

        if (payload != null) {
            for (key in payload.keys) {
                when {
                    stringClaims.contains(key) -> claimsSet.claim(key, payload[key].toString())
                }
            }
        }

        val jwk = wallet.keyPair.jwk()
        val thumb = jwk.computeThumbprint()
        claimsSet.claim("sub", BaseEncoding.base64().encode(thumb.decode()).urlSafe())
        claimsSet.claim("sub_did_verification_method_uri", wallet.keyAlias)
        claimsSet.claim("sub_jwk", jwk.toPublicJWK().toJSONObject())

        val signedJWT = SignedJWT(
            JWSHeader.Builder(JWSAlgorithm.ES256K).keyID(wallet.keyAlias).type(JOSEObjectType.JWT).build(),
            claimsSet.build()
        )

        // Compute the EC signature
        signedJWT.sign(signer)

        // Serialize the JWS to compact form
        return signedJWT.serialize()
    }

    fun verifyJWT(jwt: String, onVerified: (payload: MutableMap<String, Any>?) -> Unit) {
        Log.e("JWT verify", "starting verification")

        try {
            val parsedJWT = SignedJWT.parse(jwt)
            val header = parsedJWT.header
            val kid = header.keyID
            val payload = parsedJWT.payload.toJSONObject()
            Log.e("JWT Verify", "kid: $kid")

            val getPublicKeysRequest = EBSIRequest.get(null,null,
                {
                    val publicKeys = it.getJSONArray("publicKeys")
                    var verified = false
                    for (i in 0 until publicKeys.length()) {
                        val publicKeyStr = publicKeys.getString(i)
                        Log.e("JWT Verify", "Verifying $publicKeyStr")
                        val publicKey = KeyStoreHelper.decodePemPublicKey(publicKeyStr)
                        val verifier: JWSVerifier = ECDSAVerifier(publicKey)
                        // assertTrue(signedJWT.verify(verifier))
                        if (parsedJWT.verify(verifier)) {
                            Log.e("JWT Verify", "Verified!")
                            onVerified(payload)
                            verified = true
                        }
                    }

                    if (!verified) onVerified(null)
                }, {
                    Log.e("JWT Verify", "Failed to get kid")
                    onVerified(null)
                }).redirect(kid)

            EBSIAPI.addToQueue(getPublicKeysRequest)
        } catch (e: ParseException) {
            // Invalid signed JOSE object encoding
            Log.e("JWT Verify","Invalid JWT", e)
            onVerified(null)
        }
    }
}

fun String.urlSafe(): String {
    return this.replace("=", "").replace("+", "-").replace("/", "_")
}
