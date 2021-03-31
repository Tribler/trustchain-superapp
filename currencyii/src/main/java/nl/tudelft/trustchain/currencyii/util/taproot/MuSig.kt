package nl.tudelft.trustchain.currencyii.util.taproot

import nl.tudelft.ipv8.util.sha256
import org.bitcoinj.core.ECKey
import org.bouncycastle.math.ec.ECPoint
import java.math.BigInteger

class MuSig {

    companion object {
        public fun generate_musig_key(pubKeys: List<ECKey>): Pair<HashMap<ECKey, ByteArray>, ECPoint> {
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
