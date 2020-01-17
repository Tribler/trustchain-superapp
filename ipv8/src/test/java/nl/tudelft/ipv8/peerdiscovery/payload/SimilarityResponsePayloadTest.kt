package nl.tudelft.ipv8.peerdiscovery.payload

import nl.tudelft.ipv8.util.toHex
import org.junit.Assert.assertEquals
import org.junit.Test

class SimilarityResponsePayloadTest {
    @Test
    fun serializeAndDeserialize() {
        val identifier = 1
        val preferenceList = listOf("7e313685c1912a141279f8248fc8db5899c5df5a", "60793bdb9cc0b60c96f88069d78aee327a6241d2")
        val payload = SimilarityResponsePayload(identifier, preferenceList)
        val serialized = payload.serialize()
        assertEquals("000100027e313685c1912a141279f8248fc8db5899c5df5a60793bdb9cc0b60c96f88069d78aee327a6241d2", serialized.toHex())

        val (deserialized, size) = SimilarityResponsePayload.deserialize(serialized)
        assertEquals(serialized.size, size)
        assertEquals(identifier, deserialized.identifier)
        assertEquals(2, deserialized.preferenceList.size)
        assertEquals(preferenceList[0], deserialized.preferenceList[0])
        assertEquals(preferenceList[1], deserialized.preferenceList[1])
    }
}
