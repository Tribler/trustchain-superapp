package nl.tudelft.trustchain.common.ebsi

import android.util.Log
import com.android.volley.Response
import nl.tudelft.trustchain.common.util.TimingUtils
import org.json.JSONObject
import java.net.URI

object OnboardingTools {

    private const val TAG  = "OnbrdngTools"

    var requestTime = 0L

    private fun getVerifiableAuthorisationListener(wallet: EBSIWallet): Response.Listener<JSONObject> {
        return Response.Listener<JSONObject> { response ->
            Log.e(TAG, "Verifiable Authorisation: $response")
            wallet.storeVerifiableAuthorisation(response)
        }
    }

    private fun getAuthenticationVerificationListener(wallet: EBSIWallet, sessionToken: String, errorListener: Response.ErrorListener): VerificationListener {
        // =====ONBOARD_01_A Requests Verifiable Authorisation (VA)=====

        val authenticationVerificationListener = VerificationListener { payload ->
            if (payload != null) {
//                Log.e("PerfTest", "Verified EBSI credential in $duration ms")
                val clientId = payload["client_id"]?.toString()

                if (clientId.isNullOrEmpty()) {
                    errorListener.onErrorResponse(MyVolleyError("No client id to redirect to"))
                    return@VerificationListener
                }

//                val iss = payload["iss"].toString()
                val filteredPayload = payload.filter { JWTHelper.stringClaims.contains(it.key) }
//                val responseJWT = JWTHelper.createJWT(wallet, iss, filteredPayload, null, selfIssued = true)
                val responseJWT = JWTHelper.createJWT(wallet, clientId, filteredPayload, null, selfIssued = true)
                Log.e(TAG, "Length authentication response: ${responseJWT.length}")

                Log.e(TAG, "Response jwt: $responseJWT")

                requestTime = TimingUtils.getTimestamp()
                errorListener.toString()
                // =====ONBOARD_02A Proves control of DID key=====
                EBSIAPI.getVerifiableAuthorisation(sessionToken,
                    clientId,
                    responseJWT,
                    getVerifiableAuthorisationListener(wallet),
                    Response.ErrorListener {
                        val duration = TimingUtils.getTimestamp() - requestTime
                        Log.e("PerfTest", "EBSI auth response call in $duration ms")
                        val myVolleyError = it as MyVolleyError
                        Log.e(TAG, myVolleyError.volleyError?.networkResponse?.data?.toString(Charsets.UTF_8) ?: "No network response")
                    }
//                    errorListener
                )
            } else {
                Log.e(TAG, "Auth request verification failed")
//                Log.e("PerfTest", "Failed to verify EBSI credential in $duration ms")
            }
        }

        return authenticationVerificationListener
    }

    private fun getAuthenticationRequestListener(wallet: EBSIWallet, sessionToken: String, errorListener: Response.ErrorListener): Response.Listener<JSONObject> {
        return Response.Listener<JSONObject> { response ->
            val token = response.getString("session_token")
            val params = URI(token).splitQuery()
            val authRequestJWT = params.firstOrNull {
                it.first == "request"
            }?.second

            /*if (authRequestJWT != null) {
                Log.e("Onbrdng", "auth request jwt: $authRequestJWT")
//                val resJwt = Auditor.getService().verify(authRequestJWT, listOf(SignaturePolicy()))
                val valid = JwtService.getService().verify(authRequestJWT)
                Log.e("Onbrdng result", "JWT verification result: ${valid}")
            }

            getAuthenticationVerificationListener(wallet, sessionToken, errorListener)*/

            if (authRequestJWT != null) {
                JWTHelper.verifyJWT(authRequestJWT, getAuthenticationVerificationListener(wallet, sessionToken, errorListener))
            }
        }
    }

    fun getVerifiableAuthorisation(wallet: EBSIWallet, sessionToken: String, errorListener: Response.ErrorListener) {
       /* val did = wallet.did
        val didDocument = wallet.didDocument()

        Log.e(TAG, "did: $did")
        Log.e(TAG, "didDocument: $didDocument")
*/
        val api = "users-onboarding/v2/authentication-requests"
        val scope = "ebsi users onboarding"
        EBSIAPI.getAuthenticationRequest(api, scope, getAuthenticationRequestListener(wallet, sessionToken, errorListener), errorListener)
    }
}
