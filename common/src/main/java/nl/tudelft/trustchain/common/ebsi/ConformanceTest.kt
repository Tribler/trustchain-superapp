package nl.tudelft.trustchain.common.ebsi

import android.content.Context
import android.util.Log
import com.android.volley.Response
import kotlinx.coroutines.*
import nl.tudelft.ipv8.util.toHex
import org.json.JSONArray
import org.json.JSONObject
import java.util.*

class ConformanceTest(
    context: Context,
    private val uuid: UUID
) {
    val TAG = ConformanceTest::class.simpleName!!
    val wallet = EBSIWallet(context)
    val defaultListener = Response.Listener<JSONObject> {
        Log.e(TAG, it.toString())
    }

    private val errorListener = Response.ErrorListener {
        val myVolleyError = it as MyVolleyError
        Log.e(TAG, "Api error during conformance test (${myVolleyError.url})")
//        Log.e(TAG, myVolleyError.volleyError?.networkResponse?.allHeaders?.map { h -> "${h.name}: ${h.value}, " }?.reduce { acc, s -> acc + s } ?: "No headers")
        Log.e(TAG, myVolleyError.volleyError?.networkResponse?.data?.toString(Charsets.UTF_8) ?: "No network response")
//        Log.e(TAG, myVolleyError.volleyError?.message ?: "No message")
    }

    fun run(){
        // https://ec.europa.eu/digital-building-blocks/wikis/display/EBSIDOC/EBSI+Wallet+Conformance+Testing
        EBSIRequest.testSetup(uuid)

        onboardTest12() // get verifiable authorisation

        // onboarding 1&2 can be skipped if already performed and move on to onboarding 5
//        onboardTest5() // get access token

        // wait for access token
        CoroutineScope(Dispatchers.IO).launch {
            for (i in 1 until 5) {
                delay(2000)
//                Log.e("RegDID", "Waited ${i * 2} seconds on access token: ${wallet.accessToken}")
                if (wallet.accessToken != null) {
                    withContext(Dispatchers.Main) {
//                        onboardTest6()
                    }
                    break
                }
            }
        }

//         vaRequestTest1()

    }

    private fun onboardTest12() {
        // Let the user scan the mobile authentication token on the onboarding service page
        val onboardSessionToken = "eyJhbGciOiJFUzI1NksiLCJ0eXAiOiJKV1QifQ.eyJleHAiOjE2NTA1MjY2NzAsImlhdCI6MTY1MDUyNTc3MCwiaXNzIjoiZGlkOmVic2k6emNHdnFnWlRIQ3Rramd0Y0tSTDdIOGsiLCJvbmJvYXJkaW5nIjoicmVjYXB0Y2hhIiwidmFsaWRhdGVkSW5mbyI6eyJhY3Rpb24iOiJsb2dpbiIsImNoYWxsZW5nZV90cyI6IjIwMjItMDQtMjFUMDc6MjI6NDlaIiwiaG9zdG5hbWUiOiJhcHAuY29uZm9ybWFuY2UuaW50ZWJzaS54eXoiLCJzY29yZSI6MC43LCJzdWNjZXNzIjp0cnVlfX0.WR-M2l-4InK_WUQI1rKufBec21ql4T1iKxN9re-3ZylTF86JCDXlZWfNRZx-k9Ge0Ui2l3oOESmD0Xy4n_KxeA"
        OnboardingTools.getVerifiableAuthorisation(wallet, onboardSessionToken, errorListener)
    }

    private fun onboardTest5() {
        AuthorisationTools.getAccessToken(wallet, errorListener)
    }

    private fun onboardTest6() {
        DIDTools.registerDID(wallet, errorListener)
    }

    private fun vaRequestTest1() {
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
            put("redirect_uri", "") // empty string is regular json response
            put("client_id", "")
            put("state", "12345") //
            put("nonce", kotlin.random.Random.nextBytes(8).toHex())
            put("claims", claims.toString())
        }

        // ISSUE_011
        val conformanceServer = "https://api.conformance.intebsi.xyz"
        Log.e(TAG, "Requesting VA")
        EBSIAPI.requestVerifiableCredentialAuthorisation(api, params,
            {
                val tokenApi = "conformance/v1/issuer-mock/token"

                // val state = it.getString("state")
                val code = it.getString("code")

                // ISSUE_021
                EBSIAPI.getToken(tokenApi, code, defaultListener, errorListener, redirect = conformanceServer)
            }, errorListener, redirect = conformanceServer)
    }

    companion object {

        fun addLeading0x(s: String): String {
            return "0x$s"
        }

        fun removeLeading0x(s: String): String {
            return if (s.startsWith("0x")) {
                s.substring(2)
            } else {
                s
            }
        }

        fun printJSONObjectItems(tag: String, jsonObject: JSONObject) {
            for (key in jsonObject.keys()) {
                Log.e(tag, "$key: ${jsonObject.get(key)}")
            }
        }
    }
}
