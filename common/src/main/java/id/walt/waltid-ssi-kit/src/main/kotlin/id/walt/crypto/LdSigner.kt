package id.walt.crypto

import android.util.Log
import com.nimbusds.jose.JOSEException
import com.nimbusds.jose.JWSAlgorithm
import com.nimbusds.jose.JWSHeader
import com.nimbusds.jose.JWSSigner
import com.nimbusds.jose.crypto.ECDSASigner
import com.nimbusds.jose.crypto.RSASSASigner
import com.nimbusds.jose.crypto.impl.ECDSA
import com.nimbusds.jose.jca.JCAContext
import com.nimbusds.jose.jwk.Curve
import com.nimbusds.jose.jwk.RSAKey
import com.nimbusds.jose.util.Base64URL
import id.walt.services.crypto.CryptoService
import id.walt.services.key.KeyService
import id.walt.services.keystore.KeyType
import info.weboftrust.ldsignatures.LdProof
import info.weboftrust.ldsignatures.canonicalizer.Canonicalizers
import info.weboftrust.ldsignatures.signer.LdSigner
import info.weboftrust.ldsignatures.suites.EcdsaSecp256k1Signature2019SignatureSuite
import info.weboftrust.ldsignatures.suites.Ed25519Signature2018SignatureSuite
import info.weboftrust.ldsignatures.suites.RsaSignature2018SignatureSuite
import info.weboftrust.ldsignatures.suites.SignatureSuites
import info.weboftrust.ldsignatures.util.JWSUtil
import java.security.InvalidKeyException
import java.security.PrivateKey
import java.security.SecureRandom
import java.security.SignatureException

class LdSigner {

    // Calls Walt CryptoService via JCA WaltIdProvider
    // Currently not required since ECDSASigner is sufficient
    @Deprecated(message = "Only for testing - Use ECDSASigner instead")
    class JcaSigner(val privateKey: PrivateKey) : JWSSigner {

        private val jcaContext = JCAContext(WaltIdProvider(), SecureRandom())

        override fun getJCAContext(): JCAContext {
            return this.jcaContext
        }

        override fun supportedJWSAlgorithms(): MutableSet<JWSAlgorithm> {
            TODO("Not yet implemented")
        }

        override fun sign(header: JWSHeader, signingInput: ByteArray): Base64URL {
            val alg = header.algorithm
//            if (!supportedJWSAlgorithms().contains(alg)) {
//                throw JOSEException(AlgorithmSupportMessage.unsupportedJWSAlgorithm(alg, supportedJWSAlgorithms()))
//            }

            // DER-encoded signature, according to JCA spec
            // (sequence of two integers - R + S)
            val jcaSignature: ByteArray = try {
                val dsa = ECDSA.getSignerAndVerifier(alg, jcaContext.provider)
                dsa.initSign(privateKey, jcaContext.secureRandom)
                dsa.update(signingInput)
                dsa.sign()
            } catch (e: InvalidKeyException) {
                throw JOSEException(e.message, e)
            } catch (e: SignatureException) {
                throw JOSEException(e.message, e)
            }
            val rsByteArrayLength = ECDSA.getSignatureByteArrayLength(header.algorithm)
            val jwsSignature = ECDSA.transcodeSignatureToConcat(jcaSignature, rsByteArrayLength)
            return Base64URL.encode(jwsSignature)
        }
    }

    // Directly calls Walt CryptoService
    class JwsLtSigner(val keyId: KeyId) : JWSSigner {

        private val cryptoService = CryptoService.getService()

        override fun getJCAContext(): JCAContext {
            TODO("Not yet implemented")
        }

        override fun supportedJWSAlgorithms(): MutableSet<JWSAlgorithm> =
            HashSet(setOf(JWSAlgorithm.EdDSA, JWSAlgorithm.ES256K, JWSAlgorithm.RS256))

        override fun sign(header: JWSHeader, signingInput: ByteArray): Base64URL {


            val jcaSignature = cryptoService.sign(keyId, signingInput)
            // ED sig seems not to be DER decoding
            // val rsByteArrayLength = 64  // ECDSA.getSignatureByteArrayLength(header.algorithm)
            //  val jwsSignature = CryptoUtilJava.transcodeSignatureToConcat(jcaSignature, rsByteArrayLength)
            return Base64URL.encode(jcaSignature)
        }

    }

    class EcdsaSecp256k1Signature2019(val key: Key) : LdSigner<EcdsaSecp256k1Signature2019SignatureSuite?>(
        SignatureSuites.SIGNATURE_SUITE_ECDSASECP256L1SIGNATURE2019, null, Canonicalizers.CANONICALIZER_JCSCANONICALIZER
    ) {

        override fun sign(ldProofBuilder: LdProof.Builder<*>, signingInput: ByteArray) {
            val jwsHeader =
                JWSHeader.Builder(JWSAlgorithm.ES256K).base64URLEncodePayload(false).criticalParams(setOf("b64")).build()
            val jwsSigningInput = JWSUtil.getJwsSigningInput(jwsHeader, signingInput)
            //val jwsSigner = JcaSigner(ECPrivateKeyHandle(keyId))
//            val jwsSigner = ECDSASigner(ECPrivateKeyHandle(keyId), Curve.SECP256K1)
            Log.e("SIGNER TEST", "key: $key, alg: ${key.keyPair!!.private.algorithm} (${KeyAlgorithm.EC})")
            val jwsSigner = ECDSASigner(key.keyPair!!.private, Curve.SECP256K1)
            jwsSigner.jcaContext.provider = WaltIdProvider()
            val signature = jwsSigner.sign(jwsHeader, jwsSigningInput)
            val jws = JWSUtil.serializeDetachedJws(jwsHeader, signature)
            ldProofBuilder.jws(jws)
        }
    }

    class Ed25519Signature2018(val keyId: KeyId) : LdSigner<Ed25519Signature2018SignatureSuite?>(
        SignatureSuites.SIGNATURE_SUITE_ED25519SIGNATURE2018, null, Canonicalizers.CANONICALIZER_JCSCANONICALIZER
    ) {

        override fun sign(ldProofBuilder: LdProof.Builder<*>, signingInput: ByteArray) {
            val jwsHeader =
                JWSHeader.Builder(JWSAlgorithm.EdDSA).base64URLEncodePayload(false).criticalParams(setOf("b64")).build()
            val jwsSigningInput = JWSUtil.getJwsSigningInput(jwsHeader, signingInput)
            val signature = JwsLtSigner(keyId).sign(jwsHeader, jwsSigningInput)
            val jws = JWSUtil.serializeDetachedJws(jwsHeader, signature)

            ldProofBuilder.jws(jws)
        }
    }

    class RsaSignature2018(val keyId: KeyId) : LdSigner<RsaSignature2018SignatureSuite?>(
        SignatureSuites.SIGNATURE_SUITE_RSASIGNATURE2018, null, Canonicalizers.CANONICALIZER_JCSCANONICALIZER
    ) {
        override fun sign(ldProofBuilder: LdProof.Builder<*>, signingInput: ByteArray) {
            val jwsHeader =
                JWSHeader.Builder(JWSAlgorithm.RS256).base64URLEncodePayload(false).criticalParams(setOf("b64")).build()

            val jwsSigningInput = JWSUtil.getJwsSigningInput(jwsHeader, signingInput)
            //val jwsSigner = JcaSigner(ECPrivateKeyHandle(keyId))

            val keyService = KeyService.getService()
            val jwk = keyService.toJwk(keyId.id, KeyType.PRIVATE)


            val rsaKey = RSAKey.Builder(jwk.toPublicJWK().toRSAKey()).privateKey(jwk.toRSAKey().toRSAPrivateKey()).build()

            val jwsSigner = RSASSASigner(rsaKey.toRSAPrivateKey())
            //jwsSigner.jcaContext.provider = WaltIdProvider()

            val signature = jwsSigner.sign(jwsHeader, jwsSigningInput)
            val jws = JWSUtil.serializeDetachedJws(jwsHeader, signature)

            ldProofBuilder.jws(jws)
        }
    }
}
