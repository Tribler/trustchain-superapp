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
        // https://ec.europa.eu/digital-building-blocks/wikis/display/EBSIDOC/EBSI+Wallet+Conformance+Testing

        // Let the user scan the mobile authentication token on the onboarding service page
//        val onboardSessionToken = "eyJhbGciOiJFUzI1NksiLCJ0eXAiOiJKV1QifQ.eyJleHAiOjE2NDc0MjkxMTUsImlhdCI6MTY0NzQyODIxNSwiaXNzIjoiZGlkOmVic2k6emNHdnFnWlRIQ3Rramd0Y0tSTDdIOGsiLCJvbmJvYXJkaW5nIjoicmVjYXB0Y2hhIiwidmFsaWRhdGVkSW5mbyI6eyJhY3Rpb24iOiJsb2dpbiIsImNoYWxsZW5nZV90cyI6IjIwMjItMDMtMTZUMTA6NTY6MzJaIiwiaG9zdG5hbWUiOiJhcHAucHJlcHJvZC5lYnNpLmV1Iiwic2NvcmUiOjAuOSwic3VjY2VzcyI6dHJ1ZX19.VXO77h03VFh1JoVyd5SNwiLWaUEZkPLmb2_FfNRdeaWqhCYasyLGREWyD06PC42fzd6XyzuYb7uolJcArXfyUQ"
//        onboardTest12(onboardSessionToken)

        // onboarding 1&2 can be skipped if already performed and move on to onboarding 5
//         onboardTest5()
//        val response = JSONObject("""{"ake1_enc_payload":"0632fef89441764c4f1373677a0f4cfd03f82aeafb4024b102a02acd23c54aa4f66d5e6c1b3a08034c77e438df7821c57c42093cc529ade065283c3aab22e3b5dc292a6e663500da0d4bee1d3e1b1cfe6360a1c262682ea1e8995a239ed8db78d13264f6378ec1234e13719df990a2c41af9a4be00fb670aedfbc3cabe5faca80e88e36e0a3f79561a84df3261a810db86f09068bcc797880439d383990d8d7eb51451168bd11f3e0c3159bf6628396ddaed75a9bdd3e946604eaacf17da7f415caea4e1756798428a083389b3e824a4dcb22f6f69b9c6568de6d476cb1e35140bc82b532450a0bcc6dce50c014618d3b2fd50bad6de8a33a2dff977616d957f89f1f3d295208302c2fffab9066018ec876dfce4b22f2cd882edebf35c383c4a496d52fc152abe6c8fcda7964f955484952fc598c9c50b30a8f8ebb6b175a4d2494ad1b8683a194f52461b7b4a2f2658a656b5b2d11e60414afb0ce95ab05ad1b6737c0148a636d93430abaeb06a49971d26b2e33298d07fe07e2c801da7982d8f77c8b4fb63aa64a479440e58fffc04f1751fb988d7df0881bea0363aa83c2ecb66178c017fdae7a2c8dfcaa02fb63e1be2a36f5a82e3a95f7be2746c3ada9a42029e6ff488a280928b1916a7c0bdac33e16e06f4d6c065f7ead458470d4d6ff1a64c417d4b043eb7ce70abfe5f16e11d1fa522768c1770232eb5fb1abf2437ddc8ed98ec71b7301179e0a4ef6c549c433e64d20f830ea4e392ad7cce1a93b1d85a64dd71e001608f9e7bdfc4fb4fce20fde1ad79aaf0b55a7967886aeeb9e55452a7abff43b17dfe579bae493fe3b988209eb9b5e44beb235c9718e804207ac4d26bb178417f45253449faa60aa314967b7003f767e75d07c60feeaae6853918a2c1eecacb8e33694741b0dfd30805f750f822f38d31e57d959c29f22614097b7ca87c274875839d7a73eb7a272051ae359a2d6bae79657966316f4a80e93278f5099c122acbbb99e9d29a02ff46de7678ce2f9eb96f689ee48325d48edab12272a2408de08503e0cf3b06424ca77e19ed9431fe04c1f70c12b741f70cd858dfc2a6b868e47bbd965851fb526d9ba56b3491d66a6e5b1def3f250e5dc849cf7e","ake1_sig_payload":{"iat":1648337105,"exp":1648338005,"ake1_nonce":"1874cc08-ebae-4218-b8e5-49a0223f4ae1","ake1_enc_payload":"0632fef89441764c4f1373677a0f4cfd03f82aeafb4024b102a02acd23c54aa4f66d5e6c1b3a08034c77e438df7821c57c42093cc529ade065283c3aab22e3b5dc292a6e663500da0d4bee1d3e1b1cfe6360a1c262682ea1e8995a239ed8db78d13264f6378ec1234e13719df990a2c41af9a4be00fb670aedfbc3cabe5faca80e88e36e0a3f79561a84df3261a810db86f09068bcc797880439d383990d8d7eb51451168bd11f3e0c3159bf6628396ddaed75a9bdd3e946604eaacf17da7f415caea4e1756798428a083389b3e824a4dcb22f6f69b9c6568de6d476cb1e35140bc82b532450a0bcc6dce50c014618d3b2fd50bad6de8a33a2dff977616d957f89f1f3d295208302c2fffab9066018ec876dfce4b22f2cd882edebf35c383c4a496d52fc152abe6c8fcda7964f955484952fc598c9c50b30a8f8ebb6b175a4d2494ad1b8683a194f52461b7b4a2f2658a656b5b2d11e60414afb0ce95ab05ad1b6737c0148a636d93430abaeb06a49971d26b2e33298d07fe07e2c801da7982d8f77c8b4fb63aa64a479440e58fffc04f1751fb988d7df0881bea0363aa83c2ecb66178c017fdae7a2c8dfcaa02fb63e1be2a36f5a82e3a95f7be2746c3ada9a42029e6ff488a280928b1916a7c0bdac33e16e06f4d6c065f7ead458470d4d6ff1a64c417d4b043eb7ce70abfe5f16e11d1fa522768c1770232eb5fb1abf2437ddc8ed98ec71b7301179e0a4ef6c549c433e64d20f830ea4e392ad7cce1a93b1d85a64dd71e001608f9e7bdfc4fb4fce20fde1ad79aaf0b55a7967886aeeb9e55452a7abff43b17dfe579bae493fe3b988209eb9b5e44beb235c9718e804207ac4d26bb178417f45253449faa60aa314967b7003f767e75d07c60feeaae6853918a2c1eecacb8e33694741b0dfd30805f750f822f38d31e57d959c29f22614097b7ca87c274875839d7a73eb7a272051ae359a2d6bae79657966316f4a80e93278f5099c122acbbb99e9d29a02ff46de7678ce2f9eb96f689ee48325d48edab12272a2408de08503e0cf3b06424ca77e19ed9431fe04c1f70c12b741f70cd858dfc2a6b868e47bbd965851fb526d9ba56b3491d66a6e5b1def3f250e5dc849cf7e","did":"did:ebsi:zkouHBuuGszKYbDARCjD8bw","iss":"did:ebsi:znHeZWvhAK2FK2Dk1jXNe7m"},"ake1_jws_detached":"eyJ0eXAiOiJKV1QiLCJhbGciOiJFUzI1NksiLCJraWQiOiJodHRwczovL2FwaS5jb25mb3JtYW5jZS5pbnRlYnNpLnh5ei90cnVzdGVkLWFwcHMtcmVnaXN0cnkvdjIvYXBwcy8weDU1OWM0ZjMyZGMzNTU2NmU0YjkyYjY5NzQ5OWMzOGYzODQ3YTZjNTNmODM0NDgyMWMyNDM1NGVhZDFmMmFiMWUifQ..JjUKAYUT9Bt74kEOevzSjKa_41XbusuT_ey2zvhA9SWRHZPYre3EQVqwh-y7LcXwSTD0KWkA4zEMzcEEkyzhdw","did":"did:ebsi:znHeZWvhAK2FK2Dk1jXNe7m"}""")
//        ake1Test(response)

        registerDID()

        // vaRequestTest()

    }

    private fun onboardTest12(sessionToken: String) {
        val did = wallet.did
        val didDocument = wallet.didDocument()

        Log.e(TAG, "did: $did")
        Log.e(TAG, "didDocument: $didDocument")

        val api = "users-onboarding/v1/authentication-requests"

        // =====ONBOARD_01_A Requests Verifiable Authorisation (VA)=====

        val verifiableAuthorisationListener = Response.Listener<JSONObject> { response ->
            Log.e("VA", response.toString())
            wallet.storeVerifiableAuthorisation(response)

            // comment out if onboarding 5 is done seperately
            onboardTest5()
        }

        val authenticationVerificationListener: (MutableMap<String, Any>?) -> Unit = { payload ->
            if (payload != null) {
                val clientId = payload["client_id"]?.toString()
                val iss = payload["iss"].toString()
                val filteredPayload = payload.filter { JWTHelper.stringClaims.contains(it.key) }
                val responseJWT = JWTHelper.createJWT(wallet, iss, filteredPayload, null)

                if (!clientId.isNullOrEmpty()) {

                    // =====ONBOARD_02A Proves control of DID key=====
                    EBSIAPI.getVerifiableAuthorisation(sessionToken, clientId, responseJWT, verifiableAuthorisationListener, errorListener
                    )
                }
            } else {
                Log.e(TAG, "Auth request verification failed")
            }
        }

        val authenticationRequestListener = Response.Listener<JSONObject> { response ->
            val token = response.getString("session_token")
            val params = URI(token).splitQuery()
            val authRequestJWT = params.firstOrNull {
                it.first == "request"
            }?.second

            if (authRequestJWT != null) {
                JWTHelper.verifyJWT(authRequestJWT, authenticationVerificationListener)
            }
        }

        val scope = "ebsi users onboarding"
        EBSIAPI.getAuthenticationRequest(api, scope, authenticationRequestListener, errorListener)

    }

    private fun ake1Test(response: JSONObject) {
        wallet.getAccessToken(response) {
            /*for (key in response.keys()) {
                Log.e("AKE", "$key: ${response.get(key)}")
            }*/
        }
    }

    private fun onboardTest5() {
        // =====ONBOARD_051 Get Access Token (AT)=====
        // https://ec.europa.eu/digital-building-blocks/wikis/display/EBSIDOC/Authorisation+API

        val api = "authorisation/v1/authentication-requests"
        val scope = "openid did_authn"

        val va = wallet.verifiableAuthorisation?.getJSONObject("verifiableCredential")

        if (va == null) {
            Log.e("Onboarding5", "No verifiable authorisation yet!")
            return
        }

        Log.e("Onboarding5", "[VA] $va")

        val accessTokenListener = Response.Listener<JSONObject> { response ->
            Log.e("Ake1", response.toString())
            ake1Test(response)
        }

        // unencoded/detached signature of va
        // https://w3c-ccg.github.io/lds-ecdsa-secp256k1-2019/
        VerifiableCredentialsTools.createVerifiablePresentation(wallet, va) { vp ->
            val canonicalized = VerifiableCredentialsTools.canonicalize(vp, true)
            Log.e("Onboarding5", "[CVP] ${VerifiableCredentialsTools.canonicalize(vp)}")

            val authenticationRequestListener = Response.Listener<JSONObject> { response ->
                val uri = response.getString("uri")
                val params = URI(uri).splitQuery()

                /*params.forEach { param ->
                    Log.e("AuthReq", "${param.first}=${param.second}")
                }*/

                val authRequestJWT = params.firstOrNull {
                    it.first == "request"
                }?.second

                if (authRequestJWT != null) {
                    JWTHelper.verifyJWT(authRequestJWT) { payload ->
                        if (payload != null) {
                            val clientId = payload["client_id"]?.toString()
                            val iss = payload["iss"].toString()
                            val filteredPayload = payload.filter { JWTHelper.stringClaims.contains(it.key) }

                            val claims = mutableMapOf<String, Any>().apply {
                                put("verified_claims", canonicalized)
                                put("encryption_key", wallet.keyPair.jwk().toPublicJWK().toJSONObject())
                            }

                            val responseJWT = JWTHelper.createJWT(wallet, iss, filteredPayload, claims)
                            Log.e("JWT", responseJWT)

                            if (!clientId.isNullOrEmpty()) {

                                // =====ONBOARD_052 Share Verifiable Authorisation=====
                                EBSIAPI.getAuthorisationAccessToken(clientId, responseJWT, accessTokenListener, errorListener)
                            }
                        }
                    }
                }
            }

            EBSIAPI.getAuthenticationRequest(api, scope, authenticationRequestListener, errorListener)
        }
    }

    private fun registerDID() {
        Log.e("RegDID", "My did: ${wallet.did}")

        val didDocumentListener = Response.Listener<JSONObject> { response ->
            Log.e("Register did", "Did already exists: $response")
        }

        val didDocumentErrorListener = Response.ErrorListener {
            Log.e("RegDID", "Error getting did document")
            if (it.networkResponse.statusCode == 404) {
                Log.e("RegDID", "Status 404: ${it.message}")
            }
        }

        EBSIAPI.getDidDocument(wallet.did, didDocumentListener, didDocumentErrorListener)
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
