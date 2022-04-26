package nl.tudelft.trustchain.common.ebsi

import android.content.Context
import com.android.volley.Request
import com.android.volley.RequestQueue
import com.android.volley.Response
import com.android.volley.VolleyError
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import com.apicatalog.jsonld.JsonLdOptions
import com.apicatalog.jsonld.document.Document
import com.apicatalog.jsonld.expansion.UriExpansion
import com.apicatalog.jsonld.http.media.MediaTypeParameters
import com.apicatalog.jsonld.loader.HttpLoader
import com.apicatalog.jsonld.uri.UriUtils
import org.json.JSONObject
import java.net.URI
import java.net.URLDecoder
import java.net.URLEncoder
import java.util.*


object EBSIRequest {
    private var server = "https://api.preprod.ebsi.eu" //Change to prod server

    private var isTest = false
    private val EBSIHeaders = mutableMapOf<String, String>()
    private lateinit var requestQueue: RequestQueue

    fun setup(context: Context){
        requestQueue = Volley.newRequestQueue(context)

        // temporary binding correct java files
        HttpLoader.checkVirtualMethod()
        JsonLdOptions.checkVirtualMethod()
        Document.checkVirtualMethod()
        UriExpansion.checkVirtualMethod()
        MediaTypeParameters.checkVirtualMethod()
        UriUtils.checkVirtualMethod()
    }

    fun get(
        api: String?,
        jsonRequest: JSONObject?,
        listener: Response.Listener<JSONObject>,
        errorListener: Response.ErrorListener?,
        redirect: String? = null
    ) {
        val req = EBSIJsonObjectRequest(
            Request.Method.GET,
            apiURL(api, redirect),
            EBSIHeaders,
            jsonRequest,
            listener,
            errorListener
        )

        requestQueue.add(req)
    }

    fun post(
        api: String?,
        jsonRequest: JSONObject?,
        listener: Response.Listener<JSONObject>,
        errorListener: Response.ErrorListener?,
        redirect: String? = null
    ) {
        val req = EBSIJsonObjectRequest(
            Request.Method.POST,
            apiURL(api, redirect),
            EBSIHeaders,
            jsonRequest,
            listener,
            errorListener
        )

        requestQueue.add(req)
    }

    fun setAuthorization(sessionToken: String) {
        EBSIHeaders["Authorization"] = "Bearer $sessionToken"
    }

    fun testSetup(uuid: UUID) {
        isTest = true

        server = "https://api.conformance.intebsi.xyz"
        EBSIHeaders["Conformance"] = uuid.toString()
    }

    private fun apiURL(api: String?, redirect: String?): String {
        return (redirect ?: server) + if (api != null ) "/$api" else ""
    }

    fun urlEncodeParams(params: Map<String, String>): String {
        val paramsList = params.map {
            "${URLEncoder.encode(it.key, "UTF-8")}=${URLEncoder.encode(it.value, "UTF-8")}"
        }

        return if (paramsList.isEmpty()) {
            ""
        } else {
            paramsList.reduce { acc, s ->  "$acc&$s"}
        }
    }
}

class EBSIJsonObjectRequest(
    method: Int,
    val myUrl: String,
    private val extraHeaders: Map<String, String>?,
    jsonRequest: JSONObject?,
    listener: Response.Listener<JSONObject>,
    errorListener: Response.ErrorListener?
): JsonObjectRequest(method, myUrl, jsonRequest, listener, errorListener) {

    override fun getHeaders(): MutableMap<String, String> {
        val allHeaders = mutableMapOf<String, String>()
        allHeaders.putAll(super.getHeaders())
        allHeaders.putAll(extraHeaders ?: mapOf())
        return allHeaders
    }

    override fun deliverError(error: VolleyError?) {
//        error?.printStackTrace()
        super.deliverError(MyVolleyError(error, myUrl))
    }
}

class MyVolleyError: VolleyError {

    var volleyError: VolleyError? = null
    var url: String = "No URL"

    constructor(volleyError: VolleyError?, url: String) : super(volleyError) {
        this.volleyError = volleyError
        this.url = url
    }

    constructor(message: String): super(message)

    constructor(message: String, t: Throwable): super(message, t)
}

fun URI.splitQuery(): List<Pair<String, String?>> {
    return if (query.isNullOrEmpty()) {
        listOf()
    } else {
        query.split("&").map {
            it.splitQueryParameter()
        }
    }
}

fun String.splitQueryParameter(): Pair<String, String?> {
    val idx = this.indexOf("=")
    val key = if (idx > 0) this.substring(0, idx) else this
    val value = if (idx > 0 && this.length > idx + 1) this.substring(idx + 1) else null
    return Pair(URLDecoder.decode(key, "UTF-8"), if (value != null) URLDecoder.decode(value, "UTF-8") else null)
}
