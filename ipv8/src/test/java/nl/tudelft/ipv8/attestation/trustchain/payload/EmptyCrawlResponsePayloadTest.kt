package nl.tudelft.ipv8.attestation.trustchain.payload

import org.junit.Assert
import org.junit.Test

class EmptyCrawlResponsePayloadTest {
    @Test
    fun serializeAndDeserialize() {
        val payload = EmptyCrawlResponsePayload(
            123u
        )
        val serialized = payload.serialize()

        val (deserialized, size) = EmptyCrawlResponsePayload.deserialize(serialized)

        Assert.assertEquals(serialized.size, size)
        Assert.assertEquals(payload.crawlId, deserialized.crawlId)
    }
}
