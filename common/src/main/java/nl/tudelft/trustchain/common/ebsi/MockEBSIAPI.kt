package nl.tudelft.trustchain.common.ebsi

import com.android.volley.Response
import org.json.JSONObject

object MockEBSIAPI {

    private val TAG = MockEBSIAPI::class.simpleName!!

    // =====REQUEST VERIFIABLE ATTESTATIONS=====
    fun getCredentialIssuerAuthorisation(params: Map<String, String>,
                                 listener: Response.Listener<JSONObject>,
                                 errorListener: Response.ErrorListener?) {
        val api = "conformance/v1/issuer-mock/authorize"
        val urlParams = EBSIRequest.urlEncodeParams(params)
        val getApi = "$api?$urlParams"
        EBSIRequest.get(getApi, null, listener, errorListener)
    }
}
