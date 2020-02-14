package nl.tudelft.ipv8.attestation.trustchain

import org.junit.Assert
import org.junit.Test
import java.math.BigInteger

class TransactionEncodingTest {
    @Test
    fun encodeInt() {
        val value = 42
        val encoded = TransactionEncoding.encode(value)
        val (offset, decoded) = TransactionEncoding.decode(encoded)
        Assert.assertEquals("a2i42", encoded.toString(Charsets.UTF_8))
        Assert.assertEquals(5, offset)
        Assert.assertEquals(BigInteger("42"), decoded)
    }

    @Test
    fun encodeLong() {
        val value = 42L
        val encoded = TransactionEncoding.encode(value)
        val (offset, decoded) = TransactionEncoding.decode(encoded)
        Assert.assertEquals("a2J42", encoded.toString(Charsets.UTF_8))
        Assert.assertEquals(5, offset)
        Assert.assertEquals(value, decoded)
    }

    @Test
    fun encodeFloat() {
        val value = 4.2f
        val encoded = TransactionEncoding.encode(value)
        val (offset, decoded) = TransactionEncoding.decode(encoded)
        Assert.assertEquals("a3f4.2", encoded.toString(Charsets.UTF_8))
        Assert.assertEquals(6, offset)
        Assert.assertEquals(value, decoded)
    }

    @Test
    fun encodeString() {
        val value = "ipv8"
        val encoded = TransactionEncoding.encode(value)
        val (offset, decoded) = TransactionEncoding.decode(encoded)
        Assert.assertEquals("a4sipv8", encoded.toString(Charsets.UTF_8))
        Assert.assertEquals(7, offset)
        Assert.assertEquals(value, decoded)
    }

    @Test
    fun encodeList() {
        val value = listOf("1", "2", "3")
        val encoded = TransactionEncoding.encode(value)
        val (offset, decoded) = TransactionEncoding.decode(encoded)
        Assert.assertEquals(encoded.size, offset)
        Assert.assertEquals(value, decoded)
    }

    @Test
    fun encodeSet() {
        val value = setOf("1", "2", "3")
        val encoded = TransactionEncoding.encode(value)
        val (offset, decoded) = TransactionEncoding.decode(encoded)
        Assert.assertEquals(encoded.size, offset)
        Assert.assertEquals(value, decoded)
    }

    @Test
    fun encodeDictionary() {
        val value = mapOf(
            "foo" to "bar",
            "moo" to "milk"
        )
        val encoded = TransactionEncoding.encode(value)
        val str = encoded.toString(Charsets.UTF_8)
        val (offset, decoded) = TransactionEncoding.decode(encoded)
        Assert.assertEquals("a2d3sfoo3sbar3smoo4smilk", str)
        Assert.assertEquals(str.length, offset)
        Assert.assertEquals(value, decoded)
    }

    @Test
    fun decodeTriblerBandwidth() {
        val encoded = "a4d4bdown1i010btotal_down13i16109262778672bup7i50328238btotal_up14i12392744309837"
        val (offset, decoded) = TransactionEncoding.decode(encoded.toByteArray(Charsets.UTF_8))
        Assert.assertEquals(encoded.length, offset)
        Assert.assertEquals(mapOf(
            "down" to BigInteger("0"),
            "total_down" to BigInteger("1610926277867"),
            "up" to BigInteger("5032823"),
            "total_up" to BigInteger("12392744309837")
        ), decoded)
    }

    @Test
    fun encodeTrue() {
        val value = true
        val encoded = TransactionEncoding.encode(value)
        val (_, decoded) = TransactionEncoding.decode(encoded)
        Assert.assertEquals(decoded, value)
    }

    @Test
    fun encodeFalse() {
        val value = false
        val encoded = TransactionEncoding.encode(value)
        val (_, decoded) = TransactionEncoding.decode(encoded)
        Assert.assertEquals(decoded, value)
    }

    @Test
    fun encodeNull() {
        val value = null
        val encoded = TransactionEncoding.encode(value)
        val (_, decoded) = TransactionEncoding.decode(encoded)
        Assert.assertEquals(decoded, value)
    }
}
