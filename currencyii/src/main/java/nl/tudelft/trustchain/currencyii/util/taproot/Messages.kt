package nl.tudelft.trustchain.currencyii.util.taproot

import java.nio.ByteBuffer
import java.nio.ByteOrder

class Messages {

    companion object {
        fun serCompactSize(l: Int): ByteArray {
            var ssBuf: ByteArray = byteArrayOf()

            if (l < 253) {
                ssBuf += l.toChar().toByte()
            } else if (l < 0x10000) {
                ssBuf += 253.toChar().toByte()
                ssBuf += l.toShort().toByte()
            } else if (l < 0x100000000) {
                ssBuf += 254.toChar().toByte()
                ssBuf += l.toByte()
            } else {
                ssBuf += 255.toChar().toByte()
                ssBuf += l.toLong().toByte()
            }

            return ssBuf
        }

        fun serString(s: ByteArray): ByteArray {
            return serCompactSize(s.size) + s
        }

        fun serStringVector(l: Array<ByteArray>): ByteArray {
            var r = serCompactSize(l.size)

            for (sv in l) {
                r += serString(sv)
            }

            return r
        }

        fun deserializeVector(bytes: ByteArray, cTxIn: CTxIn): Array<CTxIn> {
            val nit = deserializeCompactSize(bytes)
            var r: Array<CTxIn> = arrayOf()
            for (i in 0..nit as Int) {
                cTxIn.deserialize(bytes)
                r += cTxIn
            }
            return r
        }

        fun deserializeVector(bytes: ByteArray, cTxOut: CTxOut): Array<CTxOut> {
            val nit = deserializeCompactSize(bytes)
            var r: Array<CTxOut> = arrayOf()
            for (i in 0..nit as Int) {
                cTxOut.deserialize(bytes)
                r += cTxOut
            }
            return r
        }

        fun deserializeStringVector(bytes: ByteArray): Array<ByteArray> {
            val nit = deserializeCompactSize(bytes)
            var r = arrayOf<ByteArray>()
            for (i in 0..nit as Int) {
                val t = deserializeString(bytes)
                r += t
            }
            return r
        }

        fun deserializeString(bytes: ByteArray): ByteArray {
            val nit = deserializeCompactSize(bytes)
            val end = when (nit) {
                is Char -> 1
                is Short -> 2
                is Int -> 4
                is Long -> 8
                else -> 0
            }
            return bytes.copyOfRange(nit as Int, (nit + end))
        }

        // TODO: This thing goes wrong
        fun deserializeCompactSize(bytes: ByteArray): Any {
            // "<B", f.read(1)
            var nit: Any =
                ByteBuffer.wrap(bytes.copyOfRange(1, 2)).order(ByteOrder.LITTLE_ENDIAN).char
            if (nit == 253) {
                // "<H", f.read(2)
                nit = ByteBuffer.wrap(bytes.copyOfRange(2, 4)).order(ByteOrder.LITTLE_ENDIAN).short
            } else if (nit == 254) {
                // "<I", f.read(4)
                nit = ByteBuffer.wrap(bytes.copyOfRange(4, 8)).order(ByteOrder.LITTLE_ENDIAN).int
            } else if (nit == 255) {
                // "<Q", f.read(8)
                nit = ByteBuffer.wrap(bytes.copyOfRange(8, 16)).order(ByteOrder.LITTLE_ENDIAN).long
            }
            return nit
        }

        fun deserializeUInt256(bytes: ByteArray): String {
            var r = ""
            for (i in 0..8) {
                val t = ByteBuffer.wrap(bytes.copyOfRange(4, 8)).order(ByteOrder.LITTLE_ENDIAN).int
                r += t.shl(i * 32)
            }
            return r
        }
    }
}
