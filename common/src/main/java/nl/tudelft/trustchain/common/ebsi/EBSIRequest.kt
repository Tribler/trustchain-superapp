package nl.tudelft.trustchain.common.ebsi

import com.android.volley.Request
import com.android.volley.Response
import com.android.volley.toolbox.JsonObjectRequest
import org.json.JSONObject
import java.net.URI
import java.net.URLDecoder
import java.net.URLEncoder
import java.util.*


object EBSIRequest {
    private var server = "https://api.preprod.ebsi.eu" //Change to prod server

    private var isTest = false
    private val EBSIHeaders = mutableMapOf<String, String>()

    fun get(
        api: String?,
        jsonRequest: JSONObject?,
        listener: Response.Listener<JSONObject>,
        errorListener: Response.ErrorListener?
    ): EBSIJsonObjectRequest {
        return EBSIJsonObjectRequest(
            Request.Method.GET,
            apiURL(api),
            EBSIHeaders,
            jsonRequest,
            listener,
            errorListener
        )
    }

    fun post(
        api: String?,
        jsonRequest: JSONObject?,
        listener: Response.Listener<JSONObject>,
        errorListener: Response.ErrorListener?
    ): EBSIJsonObjectRequest {
        return EBSIJsonObjectRequest(
            Request.Method.POST,
            apiURL(api),
            EBSIHeaders,
            jsonRequest,
            listener,
            errorListener
        )
    }

    fun setAuthorization(sessionToken: String) {
        EBSIHeaders["Authorization"] = "Bearer $sessionToken"
    }

    fun test(uuid: UUID) {
        isTest = true

        // TODO conformance server seems to have some problems
        // server = "https://api.conformance.intebsi.xyz"
        EBSIHeaders["Conformance"] = uuid.toString()
    }

    private fun apiURL(api: String?): String {
        return "$server/${api ?: ""}"
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
    url: String,
    private val extraHeaders: Map<String, String>?,
    jsonRequest: JSONObject?,
    listener: Response.Listener<JSONObject>,
    errorListener: Response.ErrorListener?
): JsonObjectRequest(method, url, jsonRequest, listener, errorListener) {

    private var redirectUrl: String? = null

    override fun getHeaders(): MutableMap<String, String> {
        val allHeaders = mutableMapOf<String, String>()
        allHeaders.putAll(super.getHeaders())
        allHeaders.putAll(extraHeaders ?: mapOf())
        return allHeaders
    }

    fun redirect(url: String): EBSIJsonObjectRequest {
        redirectUrl = url
        return this
    }

    override fun getUrl(): String {
        return redirectUrl ?: super.getUrl()
    }
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
