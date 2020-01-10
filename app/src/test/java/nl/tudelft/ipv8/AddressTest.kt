package nl.tudelft.ipv8

import nl.tudelft.ipv8.util.toHex
import org.junit.Assert.assertEquals
import org.junit.Test

class AddressTest {
    @Test
    fun serialize() {
        val address = Address("1.2.3.4", 1234)
        assertEquals("0102030404d2", address.serialize().toHex())
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
}
