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

        fun deserializeVector(bytes: ByteIterator, cTxIn: CTxIn): Array<CTxIn> {
            val nit = deserializeCompactSize(bytes)
            var r: Array<CTxIn> = arrayOf()
            for (i in 0 until nit) {
                cTxIn.deserialize(bytes)
                r += cTxIn
            }
            return r
        }

        fun deserializeVector(bytes: ByteIterator, cTxOut: CTxOut): Array<CTxOut> {
            val nit = deserializeCompactSize(bytes)
            var r: Array<CTxOut> = arrayOf()
            for (i in 0 until nit) {
                cTxOut.deserialize(bytes)
                r += cTxOut
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

        // TODO: This thing goes wrong
        fun deserializeCompactSize(bytes: ByteIterator): Long {
            // "<B", f.read(1)

            var nit: Long = ByteBuffer.wrap(read(bytes, 1)).order(ByteOrder.LITTLE_ENDIAN).get().toLong()
            if (nit == 253L) {
                // "<H", f.read(2)
                nit = ByteBuffer.wrap(read(bytes, 2)).order(ByteOrder.LITTLE_ENDIAN).short.toLong()
            } else if (nit == 254L) {
                // "<I", f.read(4)
                nit = ByteBuffer.wrap(read(bytes, 4)).order(ByteOrder.LITTLE_ENDIAN).int.toLong()
            } else if (nit == 255L) {
                // "<Q", f.read(8)
                nit = ByteBuffer.wrap(read(bytes, 8)).order(ByteOrder.LITTLE_ENDIAN).long
            }
            return nit
        }

        fun deserializeUInt256(bytes: ByteIterator): String {
            var r = ""
            for (i in 0 until 8) {
                val t = ByteBuffer.wrap(read(bytes, 4)).order(ByteOrder.LITTLE_ENDIAN).int.toUInt()
                r += t.shl(i * 32)
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
