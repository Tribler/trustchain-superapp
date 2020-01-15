package nl.tudelft.ipv8.peerdiscovery.payload

import org.junit.Assert.assertEquals
import org.junit.Test

class PongPayloadTest {
    @Test
    fun serializeAndDeserialize() {
        val identifier = 123
        val payload = PongPayload(identifier)
        val serialized = payload.serialize()

        val (deserialized, size) = PongPayload.deserialize(serialized)
        assertEquals(serialized.size, size)
        assertEquals(identifier, deserialized.identifier)
    }
}