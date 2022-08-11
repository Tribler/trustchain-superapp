package nl.tudelft.trustchain.common.ebsi

import android.util.Log
import com.android.volley.Response
import org.json.JSONObject
import java.net.URI

object AuthorisationTools {

    private const val TAG  = "AuthTools"

    private fun getAuthenticationRequestListener(
        wallet: EBSIWallet,
        canonicalizedVP: String,
        accessTokenListener: Response.Listener<JSONObject>,
        errorListener: Response.ErrorListener): Response.Listener<JSONObject> {

        return Response.Listener<JSONObject> { response ->
            val uri = response.getString("uri")
            val params = URI(uri).splitQuery()

            /*params.forEach { param ->
                Log.e(TAG, "Auth Request: ${param.first}=${param.second}")
            }*/

            val authRequestJWT = params.firstOrNull {
                it.first == "request"
            }?.second

            if (authRequestJWT == null) {
                errorListener.onErrorResponse(MyVolleyError("No authentication request jwt found"))
                return@Listener
            }

            JWTHelper.verifyJWT(authRequestJWT, VerificationListener { payload ->
                if (payload == null) {
                    errorListener.onErrorResponse(MyVolleyError("Unable to verify authentication request jwt"))
                    return@VerificationListener
                }

                val clientId = payload["client_id"]?.toString()
                val iss = payload["iss"].toString()
                val filteredPayload = payload.filter { JWTHelper.stringClaims.contains(it.key) }

                val claims = mutableMapOf<String, Any>().apply {
                    put("verified_claims", canonicalizedVP)
                    put("encryption_key", wallet.keyPair.jwk().toPublicJWK().toJSONObject())
                }

                val responseJWT = JWTHelper.createJWT(wallet, iss, filteredPayload, claims)
//                Log.e(TAG, "JWT: $responseJWT")

                if (!clientId.isNullOrEmpty()) {

                    // =====ONBOARD_052 Share Verifiable Authorisation=====
                    EBSIAPI.getAuthorisationAccessToken(
                        clientId,
                        responseJWT,
                        accessTokenListener,
                        errorListener
                    )
                } else {
                    errorListener.onErrorResponse(MyVolleyError("No client id found"))
                }
            })
        }
    }

    fun getAccessToken(wallet: EBSIWallet, errorListener: Response.ErrorListener) {
        // =====ONBOARD_051 Get Access Token (AT)=====
        // https://ec.europa.eu/digital-building-blocks/wikis/display/EBSIDOC/Authorisation+API

        val va = wallet.verifiableAuthorisation?.getJSONObject("verifiableCredential")

        if (va == null) {
            Log.e(TAG, "No verifiable authorisation yet!")
            return
        }

        val accessTokenListener = Response.Listener<JSONObject> { response ->
            wallet.getAccessTokenFromResponse(response) {
//            printJSONObjectItems("AKE", response)
            }

        }

        // unencoded/detached signature of va
        // https://w3c-ccg.github.io/lds-ecdsa-secp256k1-2019/
        VerifiableCredentialsTools.createVerifiablePresentation(wallet, va) { vp ->
            val canonicalized = VerifiableCredentialsTools.canonicalize(vp, true)

            val api = "authorisation/v1/authentication-requests"
            val scope = "openid did_authn"

            EBSIAPI.getAuthenticationRequest(api,
                scope,
                getAuthenticationRequestListener(wallet, canonicalized, accessTokenListener, errorListener),
                errorListener)
        }
    }
}
