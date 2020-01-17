package nl.tudelft.ipv8.messaging.payload

import nl.tudelft.ipv8.Address
import org.junit.Assert.assertEquals
import org.junit.Test

class PunctureRequestPayloadTest {
    @Test
    fun serialize_deserialize() {
        val sourceLanAddress = Address("1.2.3.4", 1234)
        val sourceWanAddress = Address("2.2.3.4", 2234)
        val identifier = 1
        val payload = PunctureRequestPayload(
            sourceLanAddress,
            sourceWanAddress,
            identifier
        )
        val serialized = payload.serialize()
        val (deserialized, size) = PunctureRequestPayload.deserialize(serialized)
        assertEquals(serialized.size, size)
        assertEquals(sourceLanAddress, deserialized.lanWalkerAddress)
        assertEquals(sourceWanAddress, deserialized.wanWalkerAddress)
        assertEquals(identifier, deserialized.identifier)
    }
}
