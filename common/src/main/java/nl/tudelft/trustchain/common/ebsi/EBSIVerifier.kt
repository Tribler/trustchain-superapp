package nl.tudelft.trustchain.common.ebsi

import android.util.Log
import com.android.volley.Response
import com.nimbusds.jose.jwk.Curve
import com.nimbusds.jose.jwk.ECKey
import com.nimbusds.jose.util.Base64URL
import com.nimbusds.jwt.SignedJWT
import id.walt.services.did.DidService
import org.json.JSONObject

object EBSIVerifier {
    val TAG = EBSIVerifier.javaClass.canonicalName

    fun verifyJWT(jwt: String, onVerified: VerificationListener) {
        val parsedJWT = SignedJWT.parse(jwt)
//        val payload = parsedJWT.payload.toJSONObject()
        val header = parsedJWT.header
        val kid = header.keyID

//        Log.e(TAG, "kid: $kid")
        val did = kid.split("#")[0]

//        TODO mock call to get holder did and pub key
        EBSIAPI.getDidDocument(
            did,
            {
                EBSIAPI.getDidDocument(
                    did,
                    { didDocument ->
                        Log.e(TAG, "$didDocument")
                        val verificationMethods = didDocument.getJSONArray("verificationMethod")
                        var keyFound = false
                        for (i in 0 until verificationMethods.length()) {
                            val verificationMethod = verificationMethods.getJSONObject(i)
                            if (verificationMethod.getString("id") == kid) {
                                val publicKeyJwk = verificationMethod.getJSONObject("publicKeyJwk")
                                val x = publicKeyJwk.getString("x")
                                val y = publicKeyJwk.getString("y")

                                val ecPubKey = ECKey.Builder(Curve.SECP256K1, Base64URL(x), Base64URL(y)).build().toECPublicKey()
                                JWTHelper.verifyJWT(jwt, onVerified, verificationKey = ecPubKey)
                                keyFound = true
                                break
                            }
                        }

                        if (!keyFound) {
                            onVerified(null)
                        }
                    }, {
                        val myVolleyError = it as MyVolleyError
                        Log.e(TAG, "Api error during conformance test (${myVolleyError.url})")
                        Log.e(TAG, myVolleyError.volleyError?.networkResponse?.data?.toString(Charsets.UTF_8) ?: "No network response")

                        onVerified(null)
                    })
            },
            {
                val myVolleyError = it as MyVolleyError
                Log.e(TAG, "Api error during conformance test (${myVolleyError.url})")
                Log.e(TAG, myVolleyError.volleyError?.networkResponse?.data?.toString(Charsets.UTF_8) ?: "No network response")

                onVerified(null)
            }
        )

//        val issuerDidDocument = DidService.resolveDidEbsi(kid, "https://api.conformance.intebsi.xyz/did-registry/v2/identifiers/")
//        Log.e(TAG, "$issuerDidDocument")


    }


}
