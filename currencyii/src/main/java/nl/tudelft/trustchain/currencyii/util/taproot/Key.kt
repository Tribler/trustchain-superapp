package nl.tudelft.trustchain.currencyii.util.taproot

import org.bitcoinj.core.ECKey
import org.bouncycastle.math.ec.ECPoint
import java.math.BigInteger

class Key {

    companion object {
        /**
         * Generate a random valid bip-schnorr nonce.
         * See https://github.com/bitcoinops/bips/blob/v0.1/bip-schnorr.mediawiki#Signing.
         * This implementation ensures the y-coordinate of the nonce point is a quadratic residue modulo the field size.
         */
        fun generate_schnorr_nonce(): Pair<ECKey, ECPoint> {
            val nonce_key = ECKey()
            var R = nonce_key.pubKeyPoint
            R = R.normalize()

            if (Schnorr.jacobi(R.affineYCoord.toBigInteger()) != BigInteger.ONE) {
                R = R.negate()
            }

            return Pair(nonce_key, R)
        }
    }


//    /**
//     * Convert a Jacobian point tuple p1 to affine form, or None if at infinity.
//     * An affine point is represented as the Jacobian (x, y, 1)
//     */
//    fun affine(p1: Triple<Int, Int, Int>): Triple<Int, Int, Int>? {
//        val (x1, y1, z1) = p1
//        if (z1 == 0) {
//            return null
//        }
//        val inv = modInv(z1.toDouble(), p)
//        val inv_2 = (inv!!.pow(2) % p)
//        val inv_3 = (inv_2 * inv) % p
//        return Triple((inv_2 *x1) % p,  (inv_3 * y1) % p, 1)
//    }
//
//    /**
//     * Compute the modular inverse of a modulo n
//     * See https://en.wikipedia.org/wiki/Extended_Euclidean_algorithm#Modular_integers.
//     */
//    fun modInv(a: Double, n: BigInteger): Double? {
//        var t1 = 0.0
//        var t2 = 1.0
//        var r1 = n
//        var r2 = a
//
//        while (r2 != 0.0) {
//            val q = Math.floor(r1 / r2)
//            t1 = t2
//            t2 = t1 - q * t2
//            r1 = r2
//            r2 = r1 - q * r2
//        }
//        if (r1 > 1) return null
//        if (t1 < 0) t1 += n
//        return t1
//    }

//    fun randomKey(): BigInteger {
//        val maxLimit = SECP256K1_ORDER
//        val minLimit = BigInteger("1")
//        val bigInteger: BigInteger = maxLimit.subtract(minLimit)
//        val randNum = Random()
//        val len: Int = maxLimit.bitLength()
//        var res = BigInteger(len, randNum)
//        if (res.compareTo(minLimit) < 0) res = res.add(minLimit)
//        if (res.compareTo(bigInteger) >= 0) res = res.mod(bigInteger).add(minLimit)
//        return res
//    }
}

//fun main() {
//    val maxLimit = BigInteger("115792089237316195423570985008687907852837564279074904382605163141518161494337")
//    val minLimit = BigInteger("1")
//    val bigInteger: BigInteger = maxLimit.subtract(minLimit)
//    val randNum = Random()
//    val len: Int = maxLimit.bitLength()
//    var res = BigInteger(len, randNum)
//    if (res.compareTo(minLimit) < 0) res = res.add(minLimit)
//    if (res.compareTo(bigInteger) >= 0) res = res.mod(bigInteger).add(minLimit)
//    println("The random BigInteger = $res")
//
//    val key = Key()
//    val yeet = key.generate_schnorr_nonce()
//    println(yeet.privKeyBytes.toHex())
//}

//class EllipticCurve(var p: BigInteger, var a: BigInteger, var b: BigInteger) {
//
//}
