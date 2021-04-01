package nl.tudelft.trustchain.currencyii.util.taproot

import java.util.*

class SegwitAddressKotlin {
    companion object {
        val CHARSET = "qpzry9x8gf2tvdw0s3jn54khce6mua7l"

        /**
         * Encode a segwit address
         */
        fun encode(hrp: String, witVersion: Int, witProgram: Array<Int>): String? {
            val ret: String = bech32_encode(hrp, arrayOf(witVersion) + convert_bits(witProgram, 8, 5)!!)
            val (data, decoded) = decode(hrp, ret)
            if (data == null && decoded == null) {
                return null
            }
            return ret
        }

        /**
         * Decode a segwiti address
         */
        fun decode(hrp: String, address: String): Pair<Int?, Array<Int>?> {
            val (hrpgot, data) = bech32_decode(address)
            if (hrpgot != hrp) return Pair(null, null)

            val decoded = convert_bits(data!!.copyOfRange(1, data.size), 5, 8, false)

            if (decoded == null || decoded.size < 2 || decoded.size > 40) return Pair(null, null)
            if (data[0] > 16) return Pair(null, null)
            if (data[0] == 0 && decoded.size != 20 && decoded.size != 32) return Pair(null, null)
            return Pair(data[0], decoded)
        }

        /**
         * Compute a Bech32 string given HRP and data values
         */
        fun bech32_encode(hrp: String, data: Array<Int>): String {
            val combined = data + bech32_create_checksum(hrp, data)
            var returnValue = hrp + '1'
            for (d in combined) {
                returnValue += CHARSET[d]
            }
            return returnValue
        }

        /**
         * Compute the checksum values given HRP and data.
         */
        fun bech32_create_checksum(hrp: String, data: Array<Int>): Array<Int> {
            val values = bech32_hrp_expand(hrp) + data
            val polymod = bech32_polymod(values + arrayOf(0, 0, 0, 0, 0, 0)) xor 1

            var returnValue: Array<Int> = arrayOf()
            for (i in 0..6) {
                returnValue += (polymod shr 5 * (5 - i)) and 31
            }
            return returnValue
        }

        /**
         * Validate a Bech32 string, and determine HRP and data.
         */
        fun bech32_decode(b: String): Pair<String?, Array<Int>?> {
            if (b.asSequence()
                    .any { it.toInt() < 33 || it.toInt() > 126 } || (b.toLowerCase(Locale.ROOT) != b && b.toUpperCase(
                    Locale.ROOT
                ) != b)
            ) {
                return Pair(null, null)
            }
            val bech = b.toLowerCase(Locale.ROOT)
            val pos = bech.lastIndexOf('1')
            if (pos < 1 || pos + 7 > bech.length || bech.length > 90) {
                return Pair(null, null)
            }
            if (bech.substring(pos + 1).asSequence().all { it !in CHARSET }) {
                return Pair(null, null)
            }
            val hrp = bech.substring(0, pos)
            var data: Array<Int> = arrayOf()
            for (x in bech.substring(pos + 1).asSequence()) {
                data += CHARSET.indexOf(x)
            }
            if (!bech32_verify_checksum(hrp, data)) {
                return Pair(null, null)
            }
            return Pair(hrp, data.copyOfRange(0, data.size - 6))
        }

        /**
         * Verify a checksum given HRP and converted data characters.
         */
        fun bech32_verify_checksum(hrp: String, data: Array<Int>): Boolean {
            return bech32_polymod(bech32_hrp_expand(hrp) + data) == 1
        }

        /**
         * Internal function that computes the Bech32 checksum.
         */
        private fun bech32_polymod(values: Array<Int>): Int {
            val generator = arrayOf(0x3b6a57b2, 0x26508e6d, 0x1ea119fa, 0x3d4233dd, 0x2a1462b3)
            var chk = 1
            for (value in values) {
                val top = chk shr 25
                chk = (chk and 0x1ffffff) shl 5 xor value
                for (i in 0..5) {
                    chk = chk xor when {
                        ((top shr i) and 1 != 0) -> chk xor generator[i]
                        else -> 0
                    }
                }
            }
            return chk
        }

        /**
         * Expand the HRP into values for checksum computation.
         */
        fun bech32_hrp_expand(hrp: String): Array<Int> {
            var data: Array<Int> = arrayOf()
            for (c in hrp.asSequence()) {
                data += c.toInt() shr 5
            }
            data += 0
            for (c in hrp.asSequence()) {
                data += c.toInt() and 31
            }
            return data
        }

        /**
         * General power of 2 base conversion
         */
        fun convert_bits(
            data: Array<Int>,
            fromBits: Int,
            toBits: Int,
            pad: Boolean = true
        ): Array<Int>? {
            var acc = 0
            var bits = 0
            var ret: Array<Int> = arrayOf()
            val maxv: Int = (1 shl toBits) - 1
            val max_acc: Int = (1 shl (fromBits + toBits - 1)) - 1
            for (value in data) {
//                if (value < 0 || value < (value shr fromBits)) return null
                acc = ((acc shl fromBits) or value) and max_acc
                bits += fromBits
                while (bits >= toBits) {
                    bits -= toBits
                    ret += ((acc shr bits) and maxv)
                }
            }
            if (pad) {
                if (bits != 0) {
                    ret += ((acc shl (toBits - bits) and maxv))
                }
            } else if (bits >= fromBits || ((acc shl (toBits - bits) and maxv) != 0)) {
                return null
            }
            return ret
        }
    }
}
