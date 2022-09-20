package nl.tudelft.trustchain.common.ebsi

import android.util.Log
import com.nimbusds.jose.jwk.Curve
import com.nimbusds.jose.jwk.ECKey
import com.nimbusds.jose.jwk.JWK
import com.nimbusds.jose.util.Base64URL
import id.walt.crypto.buildKey
import net.i2p.crypto.eddsa.EdDSASecurityProvider
import nl.tudelft.ipv8.android.util.AndroidEncodingUtils
import nl.tudelft.ipv8.messaging.deserializeVarLen
import nl.tudelft.ipv8.messaging.serializeVarLen
import org.spongycastle.asn1.ASN1InputStream
import org.spongycastle.asn1.DLSequence
import org.spongycastle.jcajce.provider.asymmetric.util.EC5Util
import org.spongycastle.jce.ECNamedCurveTable
import org.spongycastle.jce.ECPointUtil
import org.spongycastle.jce.provider.BouncyCastleProvider
import org.spongycastle.jce.spec.ECNamedCurveParameterSpec
import org.spongycastle.util.io.pem.PemReader
import java.io.ByteArrayInputStream
import java.io.File
import java.io.StringReader
import java.lang.Exception
import java.math.BigInteger
import java.nio.charset.Charset
import java.security.*
import java.security.interfaces.ECPrivateKey
import java.security.interfaces.ECPublicKey
import java.security.spec.*


class KeyStoreHelper(
    wallet: EBSIWallet
) {

    private val ebsiKeyFile by lazy { File(wallet.ebsiWalletDir, EBSI_KEY_FILE) }

    fun getKeyPair(): KeyPair? {

        if (ebsiKeyFile.exists()) {
            Log.e(TAG, "key files exist")

            try {
                val serialized = ebsiKeyFile.readBytes()

                var offset = 0
                val (p, pSize) = deserializeVarLen(serialized, offset)
                offset += pSize
                val (gX, gXSize) = deserializeVarLen(serialized, offset)
                offset += gXSize
                val (gY, gYSize) = deserializeVarLen(serialized, offset)
                offset += gYSize
                val (curveA, curveASize) = deserializeVarLen(serialized, offset)
                offset += curveASize
                val (curveB, curveBSize) = deserializeVarLen(serialized, offset)
                offset += curveBSize
                val (s, sSize) = deserializeVarLen(serialized, offset)
                offset += sSize
                val (n, nSize) = deserializeVarLen(serialized, offset)
                offset += nSize
                val (h, hSize) = deserializeVarLen(serialized, offset)
                offset += hSize
                val (wX, wXSize) = deserializeVarLen(serialized, offset)
                offset += wXSize
                val (wY, wYSize) = deserializeVarLen(serialized, offset)
                offset += wYSize

                val parsedP = BigInteger(p)
                val ecField = ECFieldFp(parsedP)
                val x = BigInteger(gX)
                val y = BigInteger(gY)
                val g = ECPoint(x, y)

                val a = BigInteger(curveA)
                val b = BigInteger(curveB)
                val curve = EllipticCurve(ecField, a, b)



                val bN = BigInteger(n)
                val bH = Integer.parseInt(h.toString(Charset.defaultCharset()))
                val spec = ECParameterSpec(curve, g, bN, bH)

                val bS = BigInteger(s)
                val ecPrivateKeySpec = ECPrivateKeySpec(bS, spec)

                val bWX = BigInteger(wX)
                val bWY = BigInteger(wY)
                val w = ECPoint(bWX, bWY)

                val ecPublicKeySpec = ECPublicKeySpec(w, spec)

                val kf = KeyFactory.getInstance("EC")
                val privateKey = kf.generatePrivate(ecPrivateKeySpec) as ECPrivateKey
                val publicKey = kf.generatePublic(ecPublicKeySpec) as ECPublicKey

//                val bcCurve = org.bouncycastle.math.ec.ECCurve.Fp(parsedP, a, b, bN, BigInteger.valueOf(Integer(bH).toLong()))
//                org.bouncycastle.math.ec.ECFieldElement.Fp.
//                val bcECPoint = org.bouncycastle.math.ec.ECPoint.Fp(bcCurve, bcCurve.fromBigInteger(bWX), bcCurve.fromBigInteger(bWY))

//                val bcECPoint = bcCurve.createPoint(bWX, bWY)
//                val bcEcParSpec = org.bouncycastle.jce.spec.ECParameterSpec(bcCurve,bcECPoint, bN)
//                val bcPKSpec = org.bouncycastle.jce.spec.ECPrivateKeySpec(b, bcEcParSpec)

//                val bcConf = org.bouncycastle.jce.provider.BouncyCastleProvider.CONFIGURATION
//                val bcPrivateKey = org.bouncycastle.jcajce.provider.asymmetric.ec.BCECPrivateKey("EC",ecPrivateKeySpec, bcConf)
//                val bcPublicKey = org.bouncycastle.jcajce.provider.asymmetric.ec.BCECPublicKey("EC", ecPublicKeySpec, bcConf)

//                return KeyPair(bcPublicKey, bcPrivateKey)
                return KeyPair(publicKey, privateKey)
            } catch (e: Exception) {
                Log.e(TAG, "Error reading key files", e)
            }
        }

        return null
    }

    /**
     * Stores the Keys to the keystore fle.
     * @param keyPair
     */
    fun storeKey(keyPair: KeyPair) {
        Log.e(TAG, "key file: ${ebsiKeyFile.absolutePath}")
        ebsiKeyFile.createNewFile()
        ebsiKeyFile.writeBytes(keyPair.serialize())

        Log.e(TAG, "Key pair stored")
    }

    companion object {
        private val TAG = KeyStoreHelper::class.simpleName
        const val EBSI_KEY_FILE = "EBSI_KEY"

        /**
         * The Spongy Castle Provider needs to be inserted as a provider in list of providers.
         */
        fun initProvider() {
            Security.addProvider(EdDSASecurityProvider())
            Security.addProvider(BouncyCastleProvider())

            /*for (provider in Security.getProviders()) {
                Log.e("SecServices", "=====PROVIDER: $provider (${provider.name})=====")
                for (service in provider.services) {
                    val alg = service.algorithm.lowercase()
                    if (alg.contains("eddsa") || alg.contains("25519")) {
                        Log.e("SecServices", "service: ${service.algorithm} (${service.type})")
//                Log.e("SecServices", "***********************************************")
                    }
                }
            }*/
        }

        fun decodeDerPublicKey(data: ByteArray) {
            val inStream = ByteArrayInputStream(data)
            val asn1InputStream = ASN1InputStream(inStream)
            val derObject = asn1InputStream.readObject() as DLSequence
            derObject.objects.iterator().forEach {
                Log.e(TAG, "$it")
            }
            //Log.e(TAG, "DER string: ${derObject.string}")
        }

        fun decodePemPublicKey(s: String, isBase64: Boolean = true): ECPublicKey {
            val pemString = if (isBase64) {
                AndroidEncodingUtils.decodeBase64FromString(s).toString(Charset.defaultCharset())
            } else { s }

            val pemReader = PemReader(StringReader(pemString))
            val pem = pemReader.readPemObject()
            val publicKeySpec = X509EncodedKeySpec(pem.content)
            val kf = KeyFactory.getInstance("EC")
//            val kf = KeyFactory.getInstance("Ed25519")

            return kf.generatePublic(publicKeySpec) as ECPublicKey

//            val kf = KeyFactory.getInstance("EdDSA")
//            return kf.generatePublic() as ECPublicKey
        }

        fun decodePublicKey(s: String): ByteArray {
            return AndroidEncodingUtils.decodeBase64FromString(s)
        }

        fun loadPublicKey(data: ByteArray): PublicKey? {
            val curve = "secp256k1"
            val kf = KeyFactory.getInstance("EC")

            val spec: ECNamedCurveParameterSpec = ECNamedCurveTable.getParameterSpec(curve)
            val ellipticCurve: EllipticCurve = EC5Util.convertCurve(spec.curve, spec.seed)

            val point: ECPoint = ECPointUtil.decodePoint(ellipticCurve, data)
            val params: ECParameterSpec = EC5Util.convertSpec(ellipticCurve, spec)
            val keySpec = ECPublicKeySpec(point, params)
            return kf.generatePublic(keySpec)
        }
    }
}

fun KeyPair.jwk(): JWK {
    return ECKey.Builder(Curve.SECP256K1, this.public as ECPublicKey)
        .privateKey(this.private as ECPrivateKey)
        .build()
}

fun KeyPair.serialize(): ByteArray {
    val privateKey = this.private as ECPrivateKey

    val field = privateKey.params.curve.field as ECFieldFp
    val p = field.p
    val gX = privateKey.params.generator.affineX
    val gY = privateKey.params.generator.affineY
    val curveA = privateKey.params.curve.a
    val curveB = privateKey.params.curve.b
    val s = privateKey.s
    val n = privateKey.params.order
    val h = privateKey.params.cofactor
    val publicKey = this.public as ECPublicKey
    val wX = publicKey.w.affineX
    val wY = publicKey.w.affineY

    val serialized = serializeVarLen(p.toByteArray()) +
        serializeVarLen(gX.toByteArray()) +
        serializeVarLen(gY.toByteArray()) +
        serializeVarLen(curveA.toByteArray()) +
        serializeVarLen(curveB.toByteArray()) +
        serializeVarLen(s.toByteArray()) +
        serializeVarLen(n.toByteArray()) +
        serializeVarLen("$h".toByteArray()) +
        serializeVarLen(wX.toByteArray()) +
        serializeVarLen(wY.toByteArray())

    return serialized
}
