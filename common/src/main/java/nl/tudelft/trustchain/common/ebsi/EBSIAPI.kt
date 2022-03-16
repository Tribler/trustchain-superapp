package nl.tudelft.trustchain.common.ebsi

import android.content.Context
import com.android.volley.Request
import com.android.volley.RequestQueue
import com.android.volley.Response
import com.android.volley.toolbox.Volley
import org.json.JSONObject

object EBSIAPI {

    private val TAG = EBSIAPI::class.simpleName!!
    private lateinit var requestQueue: RequestQueue

    // Move to EBSI request
    fun setup(context: Context){
        requestQueue = Volley.newRequestQueue(context)
    }

    fun addToQueue(request: EBSIJsonObjectRequest) {
        requestQueue.add(request)
    }

    // =====ONBOARDING=====
    fun getAuthenticationRequest(sessionToken: String, api: String, listener: Response.Listener<JSONObject>, errorListener: Response.ErrorListener?) {
        EBSIRequest.setAuthorization(sessionToken)
        val body = JSONObject().put("scope", "ebsi users onboarding")
        val request = EBSIRequest.post(api, body, listener, errorListener)
        requestQueue.add(request)
    }

    fun getVerifiableAuthorisation(clientId: String, idToken: String,
                                   listener: Response.Listener<JSONObject>,
                                   errorListener: Response.ErrorListener?) {
        val body = JSONObject().put("id_token", idToken)
        val request = EBSIRequest.post(null, body, listener, errorListener).redirect(clientId)
        requestQueue.add(request)
    }

    fun getAuthorisationAccessToken() {

    }

    // ===============

    // =====REQUEST VA=====

    fun requestVerifiableCredential(api: String, params: Map<String, String>,
                                    listener: Response.Listener<JSONObject>,
                                    errorListener: Response.ErrorListener?) {
        val urlParams = EBSIRequest.urlEncodeParams(params)
        val getApi = "$api?$urlParams"
        val request = EBSIRequest.get(getApi, null, listener, errorListener)
        requestQueue.add(request)
    }

    fun getToken(api: String, code: String,
                 listener: Response.Listener<JSONObject>,
                 errorListener: Response.ErrorListener?) {
        val body = JSONObject().apply {
            put("code", code)
            put("grant_type", "authorization_code")
            put("redirect_uri", "")
        }
        val request = EBSIRequest.post(api, body, listener, errorListener)
        requestQueue.add(request)
    }
    // ===============
}
