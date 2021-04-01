package nl.tudelft.trustchain.currencyii.util.taproot

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
    }
}
