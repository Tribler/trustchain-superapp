package nl.tudelft.trustchain.currencyii.util.taproot

import nl.tudelft.ipv8.util.hexToBytes
import java.nio.ByteBuffer

class Messages {

    companion object {
        fun ser_compact_size(l: Int): ByteArray {
            val ss_buf: ByteBuffer = ByteBuffer.allocate(2)

            if (l < 253) {
                ss_buf.putChar(l.toChar())
            } else if (l < 0x10000) {
                ss_buf.putChar(253.toChar())
                ss_buf.putShort(l.toShort())
            } else if (l < 0x100000000) {
                ss_buf.putChar(254.toChar())
                ss_buf.putInt(l)
            } else {
                ss_buf.putChar(255.toChar())
                ss_buf.putLong(l.toLong())
            }

            return ss_buf.array()
        }

        fun ser_string(s: String): ByteArray {
            return ser_compact_size(s.length) + s.hexToBytes()
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
