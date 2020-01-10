package nl.tudelft.ipv8.messaging.payload

import nl.tudelft.ipv8.util.toHex
import org.junit.Assert.assertEquals
import org.junit.Test

class GlobalTimeDistributionmPayloadTest {
    @Test
    fun serialize() {
        val payload = GlobalTimeDistributionPayload(1uL)
        val serialized = payload.serialize()
        assertEquals("0000000000000001", serialized.toHex())
    }

    @Test
    fun deserialize() {
        val globalTime = 1uL
        val payload = GlobalTimeDistributionPayload(globalTime)
        val serialized = payload.serialize()
        val (deserialized, size) = GlobalTimeDistributionPayload.deserialize(serialized)
        assertEquals(8, size)
        assertEquals(globalTime, deserialized.globalTime)
    }
}
