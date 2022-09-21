package nl.tudelft.trustchain.common.ebsi

import android.content.Context
import android.util.Log
import com.android.volley.Response
import com.nimbusds.jwt.SignedJWT
import id.walt.auditor.Auditor
import id.walt.auditor.SignaturePolicy
import id.walt.common.urlEncode
import id.walt.custodian.Custodian
import id.walt.model.DidMethod
import id.walt.services.did.DidService
import id.walt.services.jwt.JwtService
import id.walt.signatory.ProofConfig
import id.walt.signatory.ProofType
import id.walt.signatory.Signatory
import id.walt.vclib.model.VerifiableCredential
import kotlinx.coroutines.*
import nl.tudelft.ipv8.util.toHex
import org.apache.http.client.utils.URLEncodedUtils
import org.json.JSONArray
import org.json.JSONObject
import java.lang.Exception
import java.net.URI
import java.net.URLEncoder
import java.nio.charset.Charset
import java.security.SecureRandom
import java.time.Instant
import java.time.temporal.ChronoUnit
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
        Log.e(TAG, myVolleyError.volleyError?.networkResponse?.data?.toString(Charsets.UTF_8) ?: "No network response")
    }

    fun verCredAuthReq() {
        val token = "openid://initiate_issuance?issuer=https%3A%2F%2Fapi.conformance.intebsi.xyz%2Fconformance%2Fv2&credential_type=https%3A%2F%2Fapi.conformance.intebsi.xyz%2Ftrusted-schemas-registry%2Fv2%2Fschemas%2FzCfNxx5dMBdf4yVcsWzj1anWRuXcxrXj1aogyfN1xSu8t&conformance=8cb8d9ae-0df5-4dde-b895-967ac461b4be"
        val params = URI(token).splitQuery()
        val issuer = params.firstOrNull {
            it.first == "issuer"
        }?.second
        val credentialType = params.firstOrNull {
            it.first == "credential_type"
        }?.second
        /*val conformance = params.firstOrNull {
            it.first == "conformance"
        }?.second*/
//        val conformance = "f2634b80-37f8-4219-a060-d6e05ce7a4e3"
        val conformance = "ec77ffb6-d489-49a9-8e28-989ee8de8870"
        EBSIRequest.testSetup(UUID.fromString(conformance))

        params.forEach {
            Log.e(TAG, "initiate_issuance ${it.first}: ${it.second}")
        }

//        val redirectUri = "wallet.example.org"
        val authorizationDetails = JSONArray().apply {
            put(JSONObject().apply {
                put("type", "openid_credential")
                put("credential_type", credentialType)
                put("format", "jwt_vc")
            })
        }

//        val lh = "https://localhost:3000"
        val state = "1234abcd"
        val authorizationRequestParams = mutableMapOf<String, String>().apply {
            put("scope", "openid conformance_testing")
            put("response_type", "code")
            put("redirect_uri", "wallet.example.org") // TODO user DID on EBSI?
            put("client_id", wallet.did)
//            put("response_mode", "post")
            put("state", state) // TODO
            put("authorization_details", authorizationDetails.toString())
        }

//        val authorizationRequest = "response_type=code&client_id=$issuer&redirect_uri=$redirectUri&scope=openid&authorization_details=${urlEncode(authorizationDetails.toString())}"

        val api = "issuer-mock/authorize?${EBSIRequest.urlEncodeParams(authorizationRequestParams)}"
        Log.e(TAG, "authorizationRequest api: $api")
        EBSIRequest.stringGet(api, null, {
            Log.e(TAG, "Authorization response: $it")
        }, errorListener, issuer)

        // Authorization response
//        state=1234abcd&code=fa16847d379a8002e745

    }

    fun verCredTokReq() {
        val issuer = "https://api.conformance.intebsi.xyz/conformance/v2"
        val schema = "https://api.conformance.intebsi.xyz/trusted-schemas-registry/v2/schemas/zCfNxx5dMBdf4yVcsWzj1anWRuXcxrXj1aogyfN1xSu8t"
        val code = "862581bbd6cb00beef83"
        val conformance = "ec77ffb6-d489-49a9-8e28-989ee8de8870"
        EBSIRequest.testSetup(UUID.fromString(conformance))

        val tokenRequestParams = mutableMapOf<String, String>().apply {
            put("grant_type", "authorization_code")
            put("code", code)
            put("redirect_uri", "wallet.example.org") // TODO user DID on EBSI?
            put("code_verifier", SecureRandom.getSeed(16).toHex())
        }

        var api = "issuer-mock/token"
        Log.e(TAG, "tokenRequest api: $api")
        EBSIRequest.stringPost(api, EBSIRequest.urlEncodeParams(tokenRequestParams), { response ->
//            Log.e(TAG, "Token response: $response")
            val tokenResponse = JSONObject(response)
            printJSONObjectItems("TokResp", tokenResponse)

            val accessToken = tokenResponse.getString("access_token")
            EBSIRequest.setAuthorization(accessToken)
            val nonce = tokenResponse.getString("c_nonce")


            val credentialRequestParams = JSONObject().apply {
                val proof = JSONObject().apply {
                    put("proof_type", "jwt")
                    put("jwt", JWTHelper.createProof(wallet, wallet.did, issuer, nonce))
                }

                put("type", schema)
                put("format", "jwt_vc")
                put("proof", proof)
            }

            api = "issuer-mock/credential"
            EBSIRequest.post(api, credentialRequestParams, {
                Log.e(TAG, "credential response: $it")
            }, errorListener, issuer)
        }, errorListener, issuer)

//        RESULT
//        eyJhbGciOiJFUzI1NksiLCJ0eXAiOiJKV1QiLCJraWQiOiJkaWQ6ZWJzaTp6Y2Zjd0dqTEJvamN6dzl5aG1VRkUzWiNrZXlzLTEifQ.eyJqdGkiOiJ1cm46ZGlkOjVhMmEyNTQ1LTM1YWQtNGJlNi1iNzIyLTU5ZDhhNmIyZjFjOSIsInN1YiI6ImRpZDplYnNpOnpoV0VzcUJ3UDdRaHVoV2FYcXhqblNEIiwiaXNzIjoiZGlkOmVic2k6emNmY3dHakxCb2pjenc5eWhtVUZFM1oiLCJuYmYiOjE2NjM2Njk0NjUsImlhdCI6MTY2MzY2OTQ2NSwidmMiOnsiQGNvbnRleHQiOlsiaHR0cHM6Ly93d3cudzMub3JnLzIwMTgvY3JlZGVudGlhbHMvdjEiXSwiaWQiOiJ1cm46ZGlkOjVhMmEyNTQ1LTM1YWQtNGJlNi1iNzIyLTU5ZDhhNmIyZjFjOSIsInR5cGUiOlsiVmVyaWZpYWJsZUNyZWRlbnRpYWwiLCJWZXJpZmlhYmxlQXR0ZXN0YXRpb24iLCJWZXJpZmlhYmxlSWQiXSwiaXNzdWVyIjoiZGlkOmVic2k6emNmY3dHakxCb2pjenc5eWhtVUZFM1oiLCJpc3N1YW5jZURhdGUiOiIyMDIyLTA5LTIwVDEwOjI0OjI1WiIsInZhbGlkRnJvbSI6IjIwMjItMDktMjBUMTA6MjQ6MjVaIiwiaXNzdWVkIjoiMjAyMi0wOS0yMFQxMDoyNDoyNVoiLCJjcmVkZW50aWFsU3ViamVjdCI6eyJpZCI6ImRpZDplYnNpOnpoV0VzcUJ3UDdRaHVoV2FYcXhqblNEIiwicGVyc29uYWxJZGVudGlmaWVyIjoiSVQvREUvMTIzNCIsImZhbWlseU5hbWUiOiJDYXN0YWZpb3JpIiwiZmlyc3ROYW1lIjoiQmlhbmNhIiwiZGF0ZU9mQmlydGgiOiIxOTMwLTEwLTAxIn0sImNyZWRlbnRpYWxTY2hlbWEiOnsiaWQiOiJodHRwczovL2FwaS5jb25mb3JtYW5jZS5pbnRlYnNpLnh5ei90cnVzdGVkLXNjaGVtYXMtcmVnaXN0cnkvdjIvc2NoZW1hcy96Q2ZOeHg1ZE1CZGY0eVZjc1d6ajFhbldSdVhjeHJYajFhb2d5Zk4xeFN1OHQiLCJ0eXBlIjoiRnVsbEpzb25TY2hlbWFWYWxpZGF0b3IyMDIxIn19fQ.GZA03o_4y9yrvtwZwif8badIbQBwbzlIYFsJ1jJ5e8xF5VBMr07uOlL2kFekwgz9x5YsvnMA33H5i6mBm8qrLQ
    }


    fun run(){
        Log.e(TAG, "Conformance Test: $uuid")
        Log.e(TAG, "DID: ${wallet.did}")
        // https://ec.europa.eu/digital-building-blocks/wikis/display/EBSIDOC/EBSI+Wallet+Conformance+Testing
        EBSIRequest.testSetup(uuid)

//        ebsiVPTest()

//        verCredAuthReq()
//        verCredTokReq()

//        testKeys()

//        waltIdTest()

//        onboardTest12() // get verifiable authorisation

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

    private fun ebsiVPTest() {
        val vcJwt = EBSIWallet.MY_TEST_CREDENTIAL
        val expiration = Instant.now().plus(30, ChronoUnit.DAYS)
        val vpJwt = Custodian.getService().createPresentation(listOf(vcJwt), wallet.did, expirationDate = expiration)
        Log.e(TAG, "verifiable presentation: $vpJwt")
        val resJwt = Auditor.getService().verify(vpJwt, listOf(SignaturePolicy()))

        Log.e("Ver result", "JWT verification result: ${resJwt.valid}")
    }

    private fun testKeys() {
        val jwt = "eyJhbGciOiJFUzI1NksiLCJ0eXAiOiJKV1QifQ.eyJleHAiOjE2NjI4OTA5MTYsImlhdCI6MTY2Mjg5MDAxNiwiaXNzIjoiZGlkOmVic2k6emNHdnFnWlRIQ3Rramd0Y0tSTDdIOGsiLCJvbmJvYXJkaW5nIjoicmVjYXB0Y2hhIiwidmFsaWRhdGVkSW5mbyI6eyJhY3Rpb24iOiJsb2dpbiIsImNoYWxsZW5nZV90cyI6IjIwMjItMDktMTFUMDk6NTM6MzRaIiwiaG9zdG5hbWUiOiJhcHAucHJlcHJvZC5lYnNpLmV1Iiwic2NvcmUiOjAuOSwic3VjY2VzcyI6dHJ1ZX19.Uama77ZH8VqxvjpTf0CK05XtxgMyUdNNMAlGd2IboGAQVxY4GRoRcmo4ufTa3t3ShojAXyBOOJoTVyYHDmad4Q"
        val encodedKey = "LS0tLS1CRUdJTiBQVUJMSUMgS0VZLS0tLS0KTUZZd0VBWUhLb1pJemowQ0FRWUZLNEVFQUFvRFFnQUUvMm8zYXBIaVF4VHAxcVBPVEN6OG1sUG9tWjFsS0NodAp2bldGdVVhL1pkdGJ5d2g2TFRNd2xKUHBlRHVaYlE2R3o4cTVTek9XSVl0Z3d2OVNydVljNWc9PQotLS0tLUVORCBQVUJMSUMgS0VZLS0tLS0="

        val decodedKey = Base64.getDecoder().decode(encodedKey)
        Log.e("JWTH TEST", "Decoded FROM HEX pub key: ${decodedKey.toString(Charset.defaultCharset())}")

        val publicKey = KeyStoreHelper.decodePemPublicKey(encodedKey)
//        val publicKey = wallet.keyPair.public as ECPublicKey

//        val publicKey = buildKey("keyid", "EC", "SC", encodedKey, null).getPublicKey() as ECPublicKey

        Log.e("JWTH TEST", "Decoded PEM pub key: $publicKey")
        Log.e("JWTH TEST", "Format: ${publicKey.format}, algo: ${publicKey.algorithm}, point: ${publicKey.w}")



//        JwtService.defaultImplementation().verify(jwt, publicKey)

//        val waltKey = Key(KeyId("test_ebsi_key"), KeyAlgorithm.ECDSA_Secp256k1, CryptoProvider.SUN, publicKey)
//        KeyService.defaultImplementation().keyStore.store(waltKey)

//        JwtService.defaultImplementation().verify(jwt, waltKey)

        try {
            val parsedJwt = SignedJWT.parse(jwt)

            val verified = JwtService.defaultImplementation().secp256k1Verify(publicKey, parsedJwt)
            Log.e("ConfTest", "jwt verified: $verified")
        } catch (e: Exception) {
            Log.e("ConfTest", "Exception verifying jwt", e)
        }

        /*val verifier: JWSVerifier = ECDSAVerifier(publicKey)
        // assertTrue(signedJWT.verify(verifier))

        val parsedJWT = SignedJWT.parse(jwt)
        val verified = parsedJWT.verify(verifier)
        Log.e("JWT Verify", "Verified: $verified")*/
    }

    fun waltIdTest() {
        val issuerDid = DidService.create(DidMethod.ebsi)
        val holderDid = DidService.create(DidMethod.key)

        Log.e("WaltID Test", "Issuer: $issuerDid\nHolder: $holderDid")

        val expiration = Instant.now().plus(30, ChronoUnit.DAYS)

//        Log.e("WaltID Test", "expiration: $expiration")

        // Issue VC in JSON-LD and JWT format (for show-casing both formats)
        val vcJsonLd = Signatory.getService().issue("VerifiableId", ProofConfig(issuerDid = issuerDid, subjectDid = holderDid, proofType = ProofType.LD_PROOF, expirationDate = expiration))
        val vcJwt = Signatory.getService().issue("VerifiableId", ProofConfig(issuerDid = issuerDid, subjectDid = holderDid, proofType = ProofType.JWT, expirationDate = expiration))

//        Log.e("WaltID Test", "VC JsonLD: $vcJsonLd")
        Log.e("WaltID Test", "VC JWT: $vcJwt")

        // Present VC in JSON-LD and JWT format (for show-casing both formats)
        // expiration date is not needed when JSON-LD format
        val vpJsonLd = Custodian.getService().createPresentation(listOf(vcJsonLd), holderDid, expirationDate = null)
        val vpJwt = Custodian.getService().createPresentation(listOf(vcJwt), holderDid, expirationDate = expiration)

//        Log.e("WaltID Test", "VP JsonLD: $vpJsonLd")
        Log.e("WaltID Test", "VP JWT: $vpJwt")

        // Verify VPs, using Signature, JsonSchema and a custom policy
//        val resJsonLd = Auditor.getService().verify(vpJsonLd, listOf(SignaturePolicy(), JsonSchemaPolicy()))
        val resJsonLd = Auditor.getService().verify(vpJsonLd, listOf(SignaturePolicy()))
//        val resJwt = Auditor.getService().verify(vpJwt, listOf(SignaturePolicy(), JsonSchemaPolicy()))
        val resJwt = Auditor.getService().verify(vpJwt, listOf(SignaturePolicy()))

        Log.e("Ver result", "JSON-LD verification result: ${resJsonLd.valid}")
        Log.e("Ver result", "JWT verification result: ${resJwt.valid}")
    }

    private fun onboardTest12() {
        // Let the user scan the mobile authentication token on the onboarding service page
        val onboardSessionToken = "eyJhbGciOiJFUzI1NksiLCJ0eXAiOiJKV1QifQ.eyJleHAiOjE2NjM1MDAyMDcsImlhdCI6MTY2MzQ5OTMwNywiaXNzIjoiZGlkOmVic2k6emFBNTlzYWdXbzliWUZ6anRvNjhYc2YiLCJvbmJvYXJkaW5nIjoicmVjYXB0Y2hhIiwidmFsaWRhdGVkSW5mbyI6eyJhY3Rpb24iOiJsb2dpbiIsImNoYWxsZW5nZV90cyI6IjIwMjItMDktMThUMTE6MDg6MjNaIiwiaG9zdG5hbWUiOiJhcHAuY29uZm9ybWFuY2UuaW50ZWJzaS54eXoiLCJzY29yZSI6MC45LCJzdWNjZXNzIjp0cnVlfX0.pMmRWdbL1e7_DXoJef2BV0BKP1IEc7sA9lTV-D8c7vfgAccnwR9ZWwkIHr7TjflvwDbqy-A_7Qm1rUtUknsuLA"
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
