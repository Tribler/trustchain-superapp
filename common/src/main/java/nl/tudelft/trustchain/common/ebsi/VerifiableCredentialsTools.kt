package nl.tudelft.trustchain.common.ebsi

import android.util.Log
import com.apicatalog.jsonld.json.JsonCanonicalizer
import foundation.identity.jsonld.JsonLDObject
import info.weboftrust.ldsignatures.jsonld.LDSecurityKeywords
import info.weboftrust.ldsignatures.signer.EcdsaSecp256k1Signature2019LdSigner
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.bitcoinj.core.ECKey
import org.json.JSONArray
import org.json.JSONObject
import java.io.StringReader
import java.net.URI
import java.security.interfaces.ECPrivateKey
import java.text.SimpleDateFormat
import java.util.*


object VerifiableCredentialsTools {

    private fun getTimeStamp(): String {
        val date = Date()
        val ISO_FORMAT = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'"
        val sdf = SimpleDateFormat(ISO_FORMAT, Locale.US)
        val utc = TimeZone.getTimeZone("UTC")
        sdf.timeZone = utc
        return sdf.format(date)
    }

    fun createVerifiablePresentation(wallet: EBSIWallet, verifiableCredential: JSONObject, callback: (presentation: JSONObject) -> Unit) {
        // https://www.w3.org/TR/vc-data-model/#proofs-signatures

        CoroutineScope(Dispatchers.IO).launch {
            val subject = verifiableCredential.getJSONObject("credentialSubject").getString("id")

            val jsonLdObject = JsonLDObject.fromJson(StringReader(verifiableCredential.toString()))
            val privateECKey = ECKey.fromPrivate((wallet.privateKey as ECPrivateKey).s)
            val signer = EcdsaSecp256k1Signature2019LdSigner(privateECKey)
            signer.created = Date()
            signer.proofPurpose = LDSecurityKeywords.JSONLD_TERM_AUTHENTICATION
            signer.verificationMethod = URI.create(wallet.keyAlias)
            val ldProof = signer.sign(jsonLdObject)

            // Log.e("VerPres", "proof: ${ldProof}")

            val verifiablePresentation = JSONObject().apply {
                put("@context", JSONArray().apply { put("https://www.w3.org/2018/credentials/v1") })
                // put("id", "optional unique id")
                put("type", JSONArray().apply { put("VerifiablePresentation") })
                put("holder", subject)
                put("verifiableCredential", JSONArray().apply { put(verifiableCredential) })
                put("proof", JSONObject(ldProof.toString()))
            }

            // https://www.npmjs.com/package/canonicalize
            // base64 url

            withContext(Dispatchers.Main) {
                callback(verifiablePresentation)
            }
        }
    }

    fun canonicalize(jsonObject: JSONObject, makeBase64: Boolean = false): String {
        val jsonLdObject = JsonLDObject.fromJson(StringReader(jsonObject.toString()))
        val canonicalized = JsonCanonicalizer.canonicalize(jsonLdObject.toJsonObject())

        return if (makeBase64) {
            Base64.getUrlEncoder().encodeToString(canonicalized.toByteArray())
        } else {
            canonicalized
        }
    }

}

/*
"""
{"verifiableCredential":{
    "id":"vc:ebsi:authentication#df722250-0044-48d2-8452-93bfc2012288",
    "issuer":"did:ebsi:zcGvqgZTHCtkjgtcKRL7H8k",
    "validFrom":"2022-03-16T10:58:05Z",
    "credentialSubject":{
        "id":"did:ebsi:zkouHBuuGszKYbDARCjD8bw"
    },
    "credentialSchema":{
        "id":"https:\/\/api.conformance.intebsi.xyz\/trusted-schemas-registry\/v1\/schemas\/0x14b05b9213dbe7d343ec1fe1d3c8c739a3f3dc5a59bae55eb38fa0c295124f49",
        "type":"OID"
    },
    "issuanceDate":"2022-03-16T10:58:05Z",
    "expirationDate":"2022-09-14T10:58:05Z",
    "@context":[
        "https:\/\/www.w3.org\/2018\/credentials\/v1",
        "https:\/\/www.w3.org\/2018\/credentials\/examples\/v1",
        "https:\/\/w3c-ccg.github.io\/lds-jws2020\/contexts\/lds-jws2020-v1.json"
    ],
    "type":[
        "VerifiableCredential",
        "VerifiableAuthorisation"
    ],
    "proof":{
        "type":"EcdsaSecp256k1Signature2019",
        "created":"2022-03-16T10:58:05Z",
        "proofPurpose":"assertionMethod",
        "verificationMethod":"did:ebsi:zcGvqgZTHCtkjgtcKRL7H8k#keys-1",
        "jws":"eyJhbGciOiJFUzI1NksiLCJ0eXAiOiJKV1QifQ..RbPSprUw0-XRn4V3gzUvmcmxBYyOLONL3ipDV0fNP4SBi9oO2y_gdJixXkzBC4SSwb4XFhSIh62fB5YcQ2nGRA"
    }
}}
        """.trimIndent()
* */
