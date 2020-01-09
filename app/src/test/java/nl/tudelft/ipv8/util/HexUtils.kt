package nl.tudelft.ipv8.util

import org.junit.Assert
import org.junit.Test

class UtilsTest {
    @Test
    fun hexToBytes() {
        val txid = "d19306e0"
        val bytes = ByteArray(4)
        bytes[0] = 0xd1.toByte()
        bytes[1] = 0x93.toByte()
        bytes[2] = 0x06.toByte()
        bytes[3] = 0xe0.toByte()
        val result = txid.hexToBytes()
        Assert.assertEquals(true, result.contentEquals(bytes))
    }

    @Test
    fun bytesToHex() {
        val txid = "d19306e0"
        val bytes = ByteArray(4)
        bytes[0] = 0xd1.toByte()
        bytes[1] = 0x93.toByte()
        bytes[2] = 0x06.toByte()
        bytes[3] = 0xe0.toByte()
        Assert.assertEquals(txid, bytes.toHex())
    }

    @Test
    fun hexToBytesToHex() {
        val txid = "d19306e0"
        Assert.assertEquals(txid, txid.hexToBytes().toHex())
        Assert.assertEquals(txid, txid.toUpperCase().hexToBytes().toHex())
    }
}
