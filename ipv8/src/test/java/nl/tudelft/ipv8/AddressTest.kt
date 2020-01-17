package nl.tudelft.ipv8

import nl.tudelft.ipv8.util.toHex
import org.junit.Assert.*
import org.junit.Test

class AddressTest {
    @Test
    fun serialize() {
        val address = Address("1.2.3.4", 1234)
        assertEquals("0102030404d2", address.serialize().toHex())
    }

    @Test
    fun serialize_2() {
        val address = Address("192.168.0.1", 8090)
        val serialized = address.serialize()
        val (deserialized, _) = Address.deserialize(serialized)
        assertEquals(address.ip, deserialized.ip)
        assertEquals(address.port, deserialized.port)
    }

    @Test
    fun deserialize_1() {
        val address = Address("1.2.3.4", 0)
        val serialized = address.serialize()
        val (deserialized, size) = Address.deserialize(serialized)
        assertEquals(6, size)
        assertEquals(address, deserialized)
    }

    @Test
    fun deserialize_2() {
        val address = Address("1.2.3.4", 1234)
        val serialized = address.serialize()
        val (deserialized, _) = Address.deserialize(serialized)
        assertEquals(address, deserialized)
    }

    @Test
    fun isEmpty() {
        val emptyAddress = Address("0.0.0.0", 0)
        val nonEmptyIp = Address("1.2.3.4", 0)
        val nonEmptyPort = Address("0.0.0.0", 1)
        assertTrue(emptyAddress.isEmpty())
        assertFalse(nonEmptyIp.isEmpty())
        assertFalse(nonEmptyPort.isEmpty())
    }
}
