package nl.tudelft.trustchain.common.ebsi

import android.content.Context
import android.util.Log
import com.android.volley.Response
import org.json.JSONArray
import org.json.JSONObject
import java.net.URI
import java.util.*

class ConformanceTest(
    context: Context,
    private val uuid: UUID
) {

    val TAG = ConformanceTest::class.simpleName!!

    val wallet = EBSIWallet(context)

    val listener = Response.Listener<JSONObject> {
        Log.e(TAG, it.toString())
    }

    private val errorListener = Response.ErrorListener {
        Log.e(TAG, it.networkResponse.data.toString(Charsets.UTF_8))
    }

    fun run() {
        EBSIRequest.test(uuid)
        testWallet()
    }

    private fun testWallet(){
        // Let the user scan the mobile authentication token on the onboarding service page
        val onboardSessionToken = "eyJhbGciOiJFUzI1NksiLCJ0eXAiOiJKV1QifQ.eyJleHAiOjE2NDc0MjMwMDcsImlhdCI6MTY0NzQyMjEwNywiaXNzIjoiZGlkOmVic2k6emNHdnFnWlRIQ3Rramd0Y0tSTDdIOGsiLCJvbmJvYXJkaW5nIjoicmVjYXB0Y2hhIiwidmFsaWRhdGVkSW5mbyI6eyJhY3Rpb24iOiJsb2dpbiIsImNoYWxsZW5nZV90cyI6IjIwMjItMDMtMTZUMDk6MTU6MDRaIiwiaG9zdG5hbWUiOiJhcHAucHJlcHJvZC5lYnNpLmV1Iiwic2NvcmUiOjAuOSwic3VjY2VzcyI6dHJ1ZX19.WSAAwZ_lR62Eiw-x8WZOplEupyitJmdplzjiuCwBN9wapub-vG-WE-9gtbHVggReQnhqGO0z8S6CoeO_8FxZlA"
        onboardTest(onboardSessionToken)

        // vaRequestTest()


        //KeyStoreHelper.decodeDerPublicKey(wallet.publicKey.encoded)

        /*val appPubKey = "LS0tLS1CRUdJTiBQVUJMSUMgS0VZLS0tLS0KTUZZd0VBWUhLb1pJemowQ0FRWUZLNEVFQUFvRFFnQUVJcTU3OXJzdUhSRW50aW56OE5ubEcvZThnRGpOTlF0NApEYkNoajltQnQ3Wlg1T3YwSG5wNERzZjlRazMycnAzSXhhVmlSQ1FjVysyOE1PVVRyb3RlU1E9PQotLS0tLUVORCBQVUJMSUMgS0VZLS0tLS0K"
        val decodedPubKey = KeyStoreHelper.decodePublicKey(appPubKey)
        val pubkey = KeyStoreHelper.decodePemPublicKey(appPubKey) //KeyStoreHelper.loadPublicKey(wallet.publicKey.encoded)
        Log.e(TAG, "$pubkey")*/
    }

    private fun onboardTest(sessionToken: String) {
        val did = wallet.did
        val didDocument = wallet.didDocument()

        Log.e(TAG, "did: $did")
        Log.e(TAG, "didDocument: $didDocument")

        val api = "users-onboarding/v1/authentication-requests"

        // =====ONBOARD_01_A Requests Verifiable Authorisation (VA)=====
        EBSIAPI.getAuthenticationRequest(sessionToken, api,
            { response ->
                val token = response.getString("session_token")
                val params = URI(token).splitQuery()
                val authRequestJWT = params.firstOrNull {
                    it.first == "request"
                }?.second

                if (authRequestJWT != null) {
                    Log.e(TAG, "auth request: $authRequestJWT")
                    // val payload = JWTHelper.verifyJWT(authRequestJWT)
                     JWTHelper.verifyJWT(authRequestJWT) { payload ->
                         Log.e(TAG, "Verification callback. Payload: $payload")
                         /*payload?.forEach {
                            Log.e(TAG, "${it.key}: ${it.value}")
                        }*/

                         // if (1 == 1) return@verifyJWT

                         if (payload != null) {
                             val clientId = payload["client_id"]?.toString()
                             val iss = payload["iss"].toString()
                             val filteredPayload =
                                 payload.filter { JWTHelper.stringClaims.contains(it.key) }
                             val responseJWT = JWTHelper.createJWT(wallet, iss, filteredPayload)

                             Log.e(TAG, "Sending to $clientId -> $responseJWT")

                             if (!clientId.isNullOrEmpty()) {

                                 // =====ONBOARD_02A Proves control of DID key=====
                                 EBSIAPI.getVerifiableAuthorisation(
                                     clientId,
                                     responseJWT,
                                     listener,
                                     errorListener
                                 )
                             }
                         } else {
                             Log.e(TAG, "Auth request verification failed")
                         }
                     }
                }
            }, errorListener)

    }

    private fun vaRequestTest() {
        val api = "conformance/v1/issuer-mock/authorize"

        val credentials = JSONArray().apply {
            put(JSONObject().apply {
                put("type", "https://api.preprod.ebsi.eu/trusted-schemas-registry/v1/schemas/0x61cc4bd80d14c81778b3eb59905c8929f44a7d45e9ae6930f3bd63b98bd0557f")
                put("format", "jwt_vc")
            })

            put(JSONObject().apply {
                put("type", "https://api.preprod.ebsi.eu/trusted-schemas-registry/v1/schemas/0x1ee207961aba4a8ba018bacf3aaa338df9884d52e993468297e775a585abe4d8")
                put("format", "jwt_vc")
            })
        }

        val claims = JSONObject().put("credential", credentials)

        val params = mutableMapOf<String, String>().apply {
            put("scope", "openid")
            put("response_type", "id_token")
            put("redirect_uri", "")
            put("client_id", "")
            put("state", "12345")
            put("nonce", "56789")
            put("claims", claims.toString())
        }

        // ISSUE_011
        EBSIAPI.requestVerifiableCredential(api, params,
            {
                val tokenApi = "conformance/v1/issuer-mock/token"
                // val state = it.getString("state")
                val code = it.getString("code")

                // ISSUE_021
                EBSIAPI.getToken(tokenApi, code, listener, errorListener)
            }, errorListener)
    }
}
