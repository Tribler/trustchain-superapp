package nl.tudelft.trustchain.currencyii.util.taproot

import nl.tudelft.ipv8.util.hexToBytes
import java.nio.ByteBuffer

class Messages {

    companion object {
        fun ser_compact_size(l: Int): ByteArray {
            var ss_buf: ByteArray = byteArrayOf()

            if (l < 253) {
                ss_buf += l.toChar().toByte()
            } else if (l < 0x10000) {
                ss_buf += 253.toChar().toByte()
                ss_buf += l.toShort().toByte()
            } else if (l < 0x100000000) {
                ss_buf += 254.toChar().toByte()
                ss_buf += l.toByte()
            } else {
                ss_buf += 255.toChar().toByte()
                ss_buf += l.toLong().toByte()
            }

            return ss_buf
        }

        fun ser_string(s: ByteArray): ByteArray {
            return ser_compact_size(s.size) + s
        }

        fun ser_string_vector(l: Array<ByteArray>): ByteArray {
            var r = ser_compact_size(l.size)

            for (sv in l) {
                r += ser_string(sv)
            }

            return r
        }
    }
//    def ser_compact_size(l):
//    r = b""
//    if l < 253:
//    r = struct.pack("B", l)
//    elif l < 0x10000:
//    r = struct.pack("<BH", 253, l)
//    elif l < 0x100000000:
//    r = struct.pack("<BI", 254, l)
//    else:
//    r = struct.pack("<BQ", 255, l)
//    return r
}
