package id.walt.services.vc

import android.content.Context
import id.walt.servicematrix.ServiceProvider
import id.walt.servicematrix.ServiceRegistry
import id.walt.services.WaltIdService
import id.walt.signatory.ProofConfig
import id.walt.vclib.model.VerifiableCredential
import info.weboftrust.ldsignatures.LdProof
import kotlinx.serialization.Serializable
import java.time.Instant

enum class VerificationType {
    VERIFIABLE_CREDENTIAL,
    VERIFIABLE_PRESENTATION
}

@Serializable
data class VerificationResult(val verified: Boolean, val verificationType: VerificationType)

abstract class JsonLdCredentialService : WaltIdService() {
    override val implementation get() = serviceImplementation<JsonLdCredentialService>()

    open fun sign(jsonCred: String, config: ProofConfig): String = implementation.sign(jsonCred, config)

    open fun verify(vcOrVp: String): VerificationResult = implementation.verify(vcOrVp)
    open fun verifyVc(issuerDid: String, vc: String): Boolean = implementation.verifyVc(issuerDid, vc)
    open fun verifyVc(vcJson: String): Boolean = implementation.verifyVc(vcJson)
    open fun verifyVp(vpJson: String): Boolean = implementation.verifyVp(vpJson)

    open fun present(
        vcs: List<String>,
        holderDid: String,
        domain: String?,
        challenge: String?,
        expirationDate: Instant?
    ): String =
        implementation.present(vcs, holderDid, domain, challenge, expirationDate)

    open fun listVCs(): List<String> = implementation.listVCs()

    open fun defaultVcTemplate(): VerifiableCredential = implementation.defaultVcTemplate()

    open fun addProof(credMap: Map<String, String>, ldProof: LdProof): String = implementation.addProof(credMap, ldProof)

    open fun validateSchema(vc: VerifiableCredential, schema: String): Boolean = implementation.validateSchema(vc, schema)
    open fun validateSchemaTsr(vc: String): Boolean = implementation.validateSchemaTsr(vc)

    companion object : ServiceProvider {
        override fun getService() = object : JsonLdCredentialService() {}
        override fun defaultImplementation(context: Context) = WaltIdJsonLdCredentialService().also {
            it.androidContext = context
        }
    }
}
