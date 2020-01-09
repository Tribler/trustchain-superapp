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
    fun deserialize() {
        val address = Address("1.2.3.4", 389)
        val serialized = address.serialize()
        System.out.println(serialized.toHex())
        assertEquals(address, Address.deserialize(serialized))
    }
}
