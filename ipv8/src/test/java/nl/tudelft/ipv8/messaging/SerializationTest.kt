package nl.tudelft.ipv8.messaging

import nl.tudelft.ipv8.util.toHex
import org.junit.Test

import org.junit.Assert.*

class SerializationTest {
    @Test
    fun serializeBool_true() {
        val serialized = serializeBool(true)
        assertEquals("01", serialized.toHex())
    }

    @Test
    fun serializeBool_false() {
        val serialized = serializeBool(false)
        assertEquals("00", serialized.toHex())
    }

    @Test
    fun deserializeBool_true() {
        val serialized = serializeBool(true)
        assertEquals(true, deserializeBool(serialized))
    }

    @Test
    fun deserializeBool_false() {
        val serialized = serializeBool(false)
        assertEquals(false, deserializeBool(serialized))
    }

    @Test
    fun serializeUShort() {
        val serialized = serializeUShort(1025)
        assertEquals("0401", serialized.toHex())
    }

    @Test
    fun deserializeUShort_simple() {
        val value = 1025
        val serialized = serializeUShort(value)
        assertEquals(value, deserializeUShort(serialized))
    }

    @Test
    fun deserializeUShort_negative() {
        val value = 389
        val serialized = serializeUShort(value)
        assertEquals(value, deserializeUShort(serialized))
    }

    @Test
    fun serializeULong_small() {
        val serialized = serializeULong(1uL)
        assertEquals("0000000000000001", serialized.toHex())
    }

    @Test
    fun serializeULong_max() {
        val serialized = serializeULong(18446744073709551615uL)
        assertEquals("ffffffffffffffff", serialized.toHex())
    }

    @Test
    fun deserializeULong_test() {
        val value = 18446744073709551615uL
        val serialized = serializeULong(value)
        assertEquals(value, deserializeULong(serialized, 0))
    }

    @Test
    fun deserializeULong_test2() {
        val value = 1581459001000uL
        val serialized = serializeULong(value)
        assertEquals(value, deserializeULong(serialized, 0))
    }
}
