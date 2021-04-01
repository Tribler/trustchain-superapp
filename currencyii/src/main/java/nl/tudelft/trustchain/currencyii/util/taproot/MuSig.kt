package nl.tudelft.trustchain.currencyii.util.taproot

import nl.tudelft.ipv8.util.sha256
import org.bitcoinj.core.ECKey
import org.bouncycastle.math.ec.ECPoint
import java.math.BigInteger

/**
 * Preliminary MuSig implementation.
 *
 * Ported from python code located here: https://github.com/bitcoinops/taproot-workshop/blob/master/test_framework/musig.py
 *
 * See https://eprint.iacr.org/2018/068.pdf for the MuSig signature scheme implemented here.
 */
class MuSig {

    companion object {
        /**
         * Construct valid Schnorr signature from a list of partial MuSig signatures.
         */
        fun aggregate_musig_signatures(s_list: List<BigInteger>, aggregateNonce: ECPoint): ByteArray {
            var s_agg = BigInteger.ZERO

            for (signature in s_list) {
                s_agg = s_agg.add(signature)
            }

            s_agg = s_agg.mod(Schnorr.n)

            return aggregateNonce.xCoord.encoded.plus(s_agg.toByteArray())
        }

        /**
         * Construct a MuSig partial signature and return the s value.
         */
        fun sign_musig(
            privateKey: ECKey,
            nonceKey: ECKey,
            aggregateNonce: ECPoint,
            aggregatePublicKey: ECPoint,
            message: ByteArray
        ): BigInteger {
            assert(privateKey.pubKeyPoint.isValid)
            assert(message.size == 32)
            assert(privateKey.isCompressed)
            assert(nonceKey.privKey != BigInteger.ZERO)
            assert(Schnorr.jacobi(aggregateNonce.affineYCoord.toBigInteger()) == BigInteger.ONE)

            val e = musig_digest(aggregateNonce, aggregatePublicKey, message)
            return (nonceKey.privKey.add(privateKey.privKey.multiply(e))).mod(Schnorr.n)
        }

        /**
         * Get the digest to sign for musig
         */
        fun musig_digest(aggregateNonce: ECPoint, aggregatePublicKey: ECPoint, message: ByteArray): BigInteger {
            return BigInteger(1, sha256(aggregateNonce.xCoord.encoded.plus(aggregatePublicKey.getEncoded(true)).plus(message))).mod(Schnorr.n)
        }

        /**
         * Construct aggregated musig nonce from individually generated nonces.
         */
        fun aggregate_schnorr_nonces(nonceList: List<ECKey>): Pair<ECPoint, Boolean> {
            var r_agg: ECPoint? = null

            for (key in nonceList) {
                r_agg = if (r_agg == null) {
                    key.pubKeyPoint
                } else {
                    r_agg.add(key.pubKeyPoint)
                }
            }

            r_agg = r_agg!!.normalize()
            val r_agg_affine = Triple(r_agg.affineXCoord, r_agg.affineYCoord, 1)
            var negated = false

            if (Schnorr.jacobi(r_agg_affine.second.toBigInteger()) != BigInteger.ONE) {
                negated = true
                r_agg = r_agg.negate()
            }

            return Pair(r_agg!!, negated)
        }

        /**
         * Aggregate individually generated public keys.
         *
         * Returns a MuSig public key as defined in the MuSig paper.
         */
        fun generate_musig_key(pubKeys: List<ECKey>): Pair<HashMap<ECKey, ByteArray>, ECPoint> {
            val pubKeyList = ArrayList<BigInteger>()

            for (key in pubKeys) {
                val pubKey = key.pubKey
                pubKeyList.add(BigInteger(1, pubKey.copyOfRange(1, pubKey.size)))
            }

            val pubKeyListSorted = pubKeyList.sorted()

            var L = ByteArray(0)

            for (px in pubKeyListSorted) {
                val pxByte = px.toByteArray()

                val temp = when {
                    pxByte.size < 32 -> {
                        val residualBytes = ByteArray(32 - pxByte.size)
                        residualBytes + pxByte
                    }
                    pxByte.size == 32 -> {
                        pxByte
                    }
                    else -> {
                        pxByte.copyOfRange(pxByte.size - 32, pxByte.size)
                    }
                }

                L += temp
            }

            val Lh = sha256(L)
            val musig_c = HashMap<ECKey, ByteArray>()
            var aggregate_key: ECPoint? = null

            for (key in pubKeys) {
                val pubKey = key.pubKey
                musig_c[key] = sha256(Lh + pubKey.copyOfRange(1, pubKey.size))

                aggregate_key = if (aggregate_key == null) {
                    key.pubKeyPoint.multiply(BigInteger(1, musig_c[key]))
                } else {
                    aggregate_key.add(key.pubKeyPoint.multiply(BigInteger(1, musig_c[key])))
                }
            }

            return Pair(musig_c, aggregate_key!!)
        }
    }
}
