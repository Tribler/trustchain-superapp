package id.walt.services.jwt

import com.nimbusds.jose.*
import com.nimbusds.jose.crypto.*
import com.nimbusds.jose.crypto.impl.ECDSA
import com.nimbusds.jose.jwk.Curve
import com.nimbusds.jose.jwk.OctetKeyPair
import com.nimbusds.jose.jwk.gen.OctetKeyPairGenerator
import com.nimbusds.jwt.JWTClaimsSet
import com.nimbusds.jwt.SignedJWT
import id.walt.crypto.*
import id.walt.services.key.KeyService
import mu.KotlinLogging
import java.security.Provider
import java.security.PublicKey
import java.security.Signature
import java.security.interfaces.ECPublicKey
import java.util.*

val keyId = "123" // FIXME static keyId

open class WaltIdJwtService : JwtService() {

    private val log = KotlinLogging.logger {}

    val encKey: OctetKeyPair = OctetKeyPairGenerator(Curve.X25519).keyID(keyId).generate()

    open val keyService = KeyService.getService()
    open val provider: Provider = WaltIdProvider()

    override fun encrypt(
        kid: String, pubEncKey: OctetKeyPair, payload: String?
    ): String {

        val jweObject = JWEObject(
            JWEHeader.Builder(JWEAlgorithm.ECDH_ES, EncryptionMethod.A256GCM)
                .contentType("JWT") // required to indicate nested JWT
                .keyID(kid).build(), Payload(payload)
        )

        val pubEncKeyJwt = pubEncKey.toPublicJWK()
        val encrypter = X25519Encrypter(pubEncKeyJwt)
        // encrypter.jcaContext.provider = waltIdProvider
        jweObject.encrypt(encrypter)
        return jweObject.serialize()
    }

    fun decrypt(
        jwe: String
    ): String {
        val jweObj = JWEObject.parse(jwe)

//        val keyId = jweObj.header.keyID

        //TODO: key loading/storing
        //val encKey = KeyManagementService.load(keyId)
        /*if (encKey == null) {
            log.error { "Could not load verifying key for $keyId" }
            throw Exception("Could not load verifying key for $keyId")
        }*/
        val decrypter = X25519Decrypter(encKey)
        decrypter.jcaContext.provider = provider
        jweObj.decrypt(decrypter)

        return jweObj.payload.toString()
    }

    override fun sign(
        keyAlias: String, // verification method
        payload: String?
    ): String {

        // Default JWT claims
//        val claimsSet = JWTClaimsSet.Builder()
//            .subject("alice")
//            .issuer("https://c2id.com")
//            .expirationTime(Date(Date().getTime() + 60 * 1000))
//            .build()


        val claimsSet = if (payload != null) JWTClaimsSet.parse(payload) else JWTClaimsSet.Builder().subject(keyAlias)
            .issuer("https://walt.id").expirationTime(Date(Date().time + 60 * 1000)).build()

        val issuerKey = keyService.load(keyAlias)
        /*if (issuerKey == null) {
            log.error { "Could not load signing key for $keyAlias" }
            throw Exception("Could not load signing key for $keyAlias")
        }*/

        val jwt = when (issuerKey.algorithm) {
            KeyAlgorithm.EdDSA_Ed25519 -> {
                var jwt = SignedJWT(
                    JWSHeader.Builder(JWSAlgorithm.EdDSA).keyID(keyAlias).type(JOSEObjectType.JWT).build(), claimsSet
                )
                //jwt.sign(Ed25519Signer(issuerKey.toOctetKeyPair()))
                jwt.sign(LdSigner.JwsLtSigner(issuerKey.keyId))
                jwt
            }
            KeyAlgorithm.ECDSA_Secp256k1 -> {
                val jwt = SignedJWT(
                    JWSHeader.Builder(JWSAlgorithm.ES256K).keyID(keyAlias).type(JOSEObjectType.JWT).build(), claimsSet
                )
                val jwsSigner = ECDSASigner(ECPrivateKeyHandle(issuerKey.keyId), Curve.SECP256K1)
                jwsSigner.jcaContext.provider = provider
                jwt.sign(jwsSigner)
                jwt
            }
            KeyAlgorithm.EC -> {
                val jwt = SignedJWT(
                    JWSHeader.Builder(JWSAlgorithm.ES256K).keyID(keyAlias).type(JOSEObjectType.JWT).build(), claimsSet
                )
                val jwsSigner = ECDSASigner(ECPrivateKeyHandle(issuerKey.keyId), Curve.SECP256K1)
                jwsSigner.jcaContext.provider = provider
                jwt.sign(jwsSigner)
                jwt
            }
            else -> {
                log.error { "Algorithm ${issuerKey.algorithm} not supported" }
                throw Exception("Algorithm ${issuerKey.algorithm} not supported")
            }
        }

        val jwtStr = jwt.serialize()
        log.debug { "Signed JWT:  $jwtStr" }
        return jwtStr
    }

    override fun verify(token: String, verifierKey: Key?): Boolean {
        log.debug { "Verifying token:  $token" }
        val jwt = SignedJWT.parse(token)

        //TODO: key might also be entirely extracted out of the header",
        // Maybe resolve DID (verification method)

        var publicKey = verifierKey ?: keyService.load(jwt.header.keyID)
        log.error { "JWT key id: ${jwt.header.keyID}, public key: $publicKey" }
        log.error { "JWT key id: header: ${jwt.header}, signing input: ${jwt.signingInput}, signature: ${jwt.signature}" }

        val res = when (publicKey.algorithm) {
            KeyAlgorithm.EdDSA_Ed25519 -> jwt.verify(Ed25519Verifier(keyService.toEd25519Jwk(publicKey)))
            KeyAlgorithm.ECDSA_Secp256k1 -> {
                val verifier = ECDSAVerifier(PublicKeyHandle(publicKey.keyId, publicKey.getPublicKey() as ECPublicKey))
                verifier.jcaContext.provider = provider
                jwt.verify(verifier)
            }
            KeyAlgorithm.EC -> {
                val verifier = ECDSAVerifier(PublicKeyHandle(publicKey.keyId, publicKey.getPublicKey() as ECPublicKey))
                verifier.jcaContext.provider = provider
                jwt.verify(verifier)
            }
            else -> {
                log.error { "Algorithm ${publicKey.algorithm} not supported" }
                throw Exception("Algorithm ${publicKey.algorithm} not supported")
            }
        }

        log.debug { "JWT verified returned:  $res" }
        return res
    }

    fun secp256k1Verify(pk: PublicKey, jwt: JWSObject): Boolean {
        val jwsSignature: ByteArray = jwt.signature.decode()

        val signature: Signature = Signature.getInstance("SHA256withECDSA", "SC")
        signature.initVerify(pk)
        signature.update(jwt.signingInput)
        return signature.verify(ECDSA.transcodeSignatureToDER(jwsSignature))
    }

    override fun parseClaims(token: String): MutableMap<String, Any>? {
        val jwt = SignedJWT.parse(token)
        val claimsMap = jwt.jwtClaimsSet.claims
        return claimsMap
    }

}
