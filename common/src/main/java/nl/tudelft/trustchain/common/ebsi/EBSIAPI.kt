package nl.tudelft.trustchain.common.ebsi

import com.android.volley.Response
import org.json.JSONObject
import java.net.URLEncoder

object EBSIAPI {

    private val TAG = EBSIAPI::class.simpleName!!

    // =====ONBOARDING=====
    fun getAuthenticationRequest(api: String,
                                 scope: String,
                                 listener: Response.Listener<JSONObject>,
                                 errorListener: Response.ErrorListener?) {
        val body = JSONObject().put("scope", scope)
        EBSIRequest.post(api, body, listener, errorListener)
    }

    fun getVerifiableAuthorisation(sessionToken: String, clientId: String, idToken: String,
                                   listener: Response.Listener<JSONObject>,
                                   errorListener: Response.ErrorListener?) {
        EBSIRequest.setAuthorization(sessionToken)
        val body = JSONObject().put("id_token", idToken)
        EBSIRequest.post(null, body, listener, errorListener, redirect = clientId)
    }

    fun getAuthorisationAccessToken(clientId: String, idToken: String,
                                    listener: Response.Listener<JSONObject>,
                                    errorListener: Response.ErrorListener?) {
        val body = JSONObject().put("id_token", idToken)
        EBSIRequest.post(null, body, listener, errorListener, redirect = clientId)
    }

    // ===============

    // =====REQUEST VA=====

    fun requestVerifiableCredentialAuthorisation(api: String, params: Map<String, String>,
                                                 listener: Response.Listener<JSONObject>,
                                                 errorListener: Response.ErrorListener?, redirect: String? = null) {
        val urlParams = EBSIRequest.urlEncodeParams(params)
        val getApi = "$api?$urlParams"
        EBSIRequest.get(getApi, null, listener, errorListener, redirect)
    }

    fun getToken(api: String, code: String,
                 listener: Response.Listener<JSONObject>,
                 errorListener: Response.ErrorListener?, redirect: String? = null) {
        val body = JSONObject().apply {
            put("code", code)
            put("grant_type", "authorization_code")
            put("redirect_uri", "")
        }
        EBSIRequest.post(api, body, listener, errorListener, redirect)
    }
    // ===============

    // =====DID Registry=====
    fun getDidDocument(did: String,
                       listener: Response.Listener<JSONObject>,
                       errorListener: Response.ErrorListener?) {
        val api = "did-registry/v2/identifiers/${URLEncoder.encode(did, "UTF-8")}"
        EBSIRequest.get(api, null, listener, errorListener)
    }

    fun jsonRpc(rpcBody: JSONObject,
                listener: Response.Listener<JSONObject>,
                errorListener: Response.ErrorListener?) {
        val api = "did-registry/v2/jsonrpc"
        EBSIRequest.post(api, rpcBody, listener, errorListener)
    }

    // ===============
}
