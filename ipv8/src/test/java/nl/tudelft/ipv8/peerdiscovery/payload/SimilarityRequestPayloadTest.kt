package nl.tudelft.ipv8.peerdiscovery.payload

import nl.tudelft.ipv8.Address
import nl.tudelft.ipv8.messaging.payload.ConnectionType
import nl.tudelft.ipv8.util.toHex
import org.junit.Assert.assertEquals
import org.junit.Test

class SimilarityRequestPayloadTest {
    @Test
    fun serializeAndDeserialize() {
        val identifier = 1
        val lan = Address("1.2.3.4", 1234)
        val wan = Address("2.2.3.4", 2234)
        val connectionType = ConnectionType.UNKNOWN
        val preferenceList = listOf("7e313685c1912a141279f8248fc8db5899c5df5a", "60793bdb9cc0b60c96f88069d78aee327a6241d2")
        val payload = SimilarityRequestPayload(identifier, lan, wan, connectionType, preferenceList)
        val serialized = payload.serialize()
        assertEquals("00010102030404d20202030408ba007e313685c1912a141279f8248fc8db5899c5df5a60793bdb9cc0b60c96f88069d78aee327a6241d2", serialized.toHex())

        val (deserialized, size) = SimilarityRequestPayload.deserialize(serialized)
        assertEquals(serialized.size, size)
        assertEquals(identifier, deserialized.identifier)
        assertEquals(lan, deserialized.lanAddress)
        assertEquals(wan, deserialized.wanAddress)
        assertEquals(connectionType, deserialized.connectionType)
        assertEquals(2, deserialized.preferenceList.size)
        assertEquals(preferenceList[0], deserialized.preferenceList[0])
        assertEquals(preferenceList[1], deserialized.preferenceList[1])
    }
}
