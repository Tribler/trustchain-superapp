package nl.tudelft.ipv8.peerdiscovery.payload

import nl.tudelft.ipv8.util.toHex
import org.junit.Assert.assertEquals
import org.junit.Test

class PongPayloadTest {
    @Test
    fun serializeAndDeserialize() {
        val identifier = 123
        val payload = PongPayload(identifier)
        val serialized = payload.serialize()
        assertEquals("007b", serialized.toHex())

        val (deserialized, size) = PongPayload.deserialize(serialized)
        assertEquals(serialized.size, size)
        assertEquals(identifier, deserialized.identifier)
    }
}
