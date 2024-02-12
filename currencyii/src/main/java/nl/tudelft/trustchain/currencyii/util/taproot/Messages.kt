package nl.tudelft.trustchain.currencyii.util.taproot

import nl.tudelft.ipv8.util.toHex
import java.nio.ByteBuffer
import java.nio.ByteOrder

class Messages {

    @Suppress("DEPRECATION")
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

        fun deserializeVectorCTxIn(bytes: ByteIterator): Array<CTxIn> {
            val nit = deserializeCompactSize(bytes)
            var r: Array<CTxIn> = arrayOf()
            for (i in 0 until nit) {
                r += CTxIn().deserialize(bytes)
            }
            return r
        }

        fun deserializeVectorCTxOut(bytes: ByteIterator): Array<CTxOut> {
            val nit = deserializeCompactSize(bytes)
            var r: Array<CTxOut> = arrayOf()
            for (i in 0 until nit) {
                r += CTxOut().deserialize(bytes)
            }
            return r
        }

        fun deserializeStringVector(bytes: ByteIterator): Array<ByteArray> {
            val nit = deserializeCompactSize(bytes)
            var r = arrayOf<ByteArray>()
            for (i in 0 until nit) {
                val t = deserializeString(bytes)
                r += t
            }
            return r
        }

        fun deserializeString(bytes: ByteIterator): ByteArray {
            val nit = deserializeCompactSize(bytes)
            return read(bytes, nit)
        }

        fun deserializeCompactSize(bytes: ByteIterator): Long {
            var nit: Long = ByteBuffer.wrap(read(bytes, 1)).order(ByteOrder.LITTLE_ENDIAN).get().toLong()
            if (nit == 253L) {
                nit = ByteBuffer.wrap(read(bytes, 2)).order(ByteOrder.LITTLE_ENDIAN).short.toLong()
            } else if (nit == 254L) {
                nit = ByteBuffer.wrap(read(bytes, 4)).order(ByteOrder.LITTLE_ENDIAN).int.toLong()
            } else if (nit == 255L) {
                nit = ByteBuffer.wrap(read(bytes, 8)).order(ByteOrder.LITTLE_ENDIAN).long
            }
            return nit
        }

        fun deserializeUInt256(bytes: ByteIterator): String {
            var r = ""
            for (i in 0 until 8) {
                val t = ByteBuffer.wrap(read(bytes, 4)).order(ByteOrder.LITTLE_ENDIAN).int
                r = t.shl(i * 32).toBigInteger().toByteArray().toHex() + r
            }
            return r
        }

        fun read(bytes: ByteIterator, numberOfBytes: Long): ByteArray {
            var byteArray = byteArrayOf()
            for (i in 0 until numberOfBytes) {
                byteArray += bytes.nextByte()
            }
            return byteArray
        }
    }
}
