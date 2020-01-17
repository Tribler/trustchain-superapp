package nl.tudelft.ipv8.peerdiscovery.payload

import nl.tudelft.ipv8.util.toHex
import org.junit.Assert.assertEquals
import org.junit.Test

class PingPayloadTest {
    @Test
    fun serializeAndDeserialize() {
        val identifier = 123
        val payload = PingPayload(identifier)
        val serialized = payload.serialize()
        assertEquals("007b", serialized.toHex())

        val (deserialized, size) = PingPayload.deserialize(serialized)
        assertEquals(serialized.size, size)
        assertEquals(identifier, deserialized.identifier)
    }

    @Test
    fun deserializePacket() {
        // 00027e313685c1912a141279f8248fc8db5899c5df5a0300000000000000010001
    }
}
