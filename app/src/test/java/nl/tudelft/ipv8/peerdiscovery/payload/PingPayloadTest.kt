package nl.tudelft.ipv8.peerdiscovery.payload

import org.junit.Assert.assertEquals
import org.junit.Test

class PingPayloadTest {
    @Test
    fun serializeAndDeserialize() {
        val identifier = 123
        val payload = PingPayload(identifier)
        val serialized = payload.serialize()

        val (deserialized, size) = PingPayload.deserialize(serialized)
        assertEquals(serialized.size, size)
        assertEquals(identifier, deserialized.identifier)
    }
}