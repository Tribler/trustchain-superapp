package nl.tudelft.trustchain.musicdao.core.util.taproot

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
        fun aggregateMusigSignatures(
            s_list: List<BigInteger>,
            aggregateNonce: ECPoint
        ): ByteArray {
            var sAgg = BigInteger.ZERO

            for (signature in s_list) {
                sAgg = sAgg.add(signature)
            }

            sAgg = sAgg.mod(Schnorr.n)

            return aggregateNonce.xCoord.encoded.plus(sAgg.toByteArray())
        }

        /**
         * Construct a MuSig partial signature and return the s value.
         */
        fun signMusig(
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

            val e = musigDigest(aggregateNonce, aggregatePublicKey, message)
            return (nonceKey.privKey.add(privateKey.privKey.multiply(e))).mod(Schnorr.n)
        }

        /**
         * Get the digest to sign for musig
         */
        fun musigDigest(
            aggregateNonce: ECPoint,
            aggregatePublicKey: ECPoint,
            message: ByteArray
        ): BigInteger {
            return BigInteger(
                1,
                sha256(aggregateNonce.xCoord.encoded.plus(aggregatePublicKey.getEncoded(true)).plus(message))
            ).mod(Schnorr.n)
        }

        /**
         * Construct aggregated musig nonce from individually generated nonces.
         */
        fun aggregateSchnorrNonces(nonceList: List<ECKey>): Pair<ECPoint, Boolean> {
            var rAgg: ECPoint? = null

            for (key in nonceList) {
                rAgg =
                    if (rAgg == null) {
                        key.pubKeyPoint
                    } else {
                        rAgg.add(key.pubKeyPoint)
                    }
            }

            rAgg = rAgg!!.normalize()
            val rAggAffine = Triple(rAgg.affineXCoord, rAgg.affineYCoord, 1)
            var negated = false

            if (Schnorr.jacobi(rAggAffine.second.toBigInteger()) != BigInteger.ONE) {
                negated = true
                rAgg = rAgg.negate()
            }

            return Pair(rAgg!!, negated)
        }

        /**
         * Aggregate individually generated public keys.
         *
         * Returns a MuSig public key as defined in the MuSig paper.
         */
        fun generateMusigKey(pubKeys: List<ECKey>): Pair<HashMap<ECKey, ByteArray>, ECPoint> {
            val pubKeyList = ArrayList<BigInteger>()

            for (key in pubKeys) {
                val pubKey = key.pubKey
                pubKeyList.add(BigInteger(1, pubKey.copyOfRange(1, pubKey.size)))
            }

            val pubKeyListSorted = pubKeyList.sorted()

            var l = ByteArray(0)

            for (px in pubKeyListSorted) {
                val pxByte = px.toByteArray()

                val temp =
                    when {
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

                l += temp
            }

            val lh = sha256(l)
            val musigC = HashMap<ECKey, ByteArray>()
            var aggregateKey: ECPoint? = null

            for (key in pubKeys) {
                val pubKey = key.pubKey
                musigC[key] = sha256(lh + pubKey.copyOfRange(1, pubKey.size))

                aggregateKey =
                    if (aggregateKey == null) {
                        key.pubKeyPoint.multiply(BigInteger(1, musigC[key]))
                    } else {
                        aggregateKey.add(key.pubKeyPoint.multiply(BigInteger(1, musigC[key])))
                    }
            }

            return Pair(musigC, aggregateKey!!)
        }
    }
}
