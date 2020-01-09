package nl.tudelft.ipv8.messaging.payload

import nl.tudelft.ipv8.Address
import nl.tudelft.ipv8.util.toHex
import org.junit.Assert.assertEquals
import org.junit.Test

class IntroductionRequestPayloadTest {
    @Test
    fun serialize() {
        val payload = IntroductionRequestPayload(
            Address("1.2.3.4", 1234),
            Address("2.2.3.4", 2234),
            Address("3.2.3.4", 3234),
            true,
            ConnectionType.UNKNOWN,
            1
        )
        val serialized = payload.serialize()
        assertEquals("0102030404d20202030408ba030203040ca2010001", serialized.toHex())
    }

    @Test
    fun deserialize() {
        val destinationAddress = Address("1.2.3.4", 1234)
        val sourceLanAddress = Address("2.2.3.4", 2234)
        val sourceWanAddress = Address("3.2.3.4", 3234)
        val payload = IntroductionRequestPayload(
            destinationAddress,
            sourceLanAddress,
            sourceWanAddress,
            true,
            ConnectionType.UNKNOWN,
            1
        )
        val serialized = payload.serialize()
        val deserialized = IntroductionRequestPayload.deserialize(serialized)
        assertEquals(destinationAddress, deserialized.destinationAddress)
        assertEquals(sourceLanAddress, deserialized.sourceLanAddress)
        assertEquals(sourceWanAddress, deserialized.sourceWanAddress)
        assertEquals(true, deserialized.advice)
        assertEquals(ConnectionType.UNKNOWN, deserialized.connectionType)
        assertEquals(1, deserialized.identifier)
    }

    @Test
    fun connectionType() {
        val connectionType = ConnectionType.PUBLIC
        val payload = IntroductionRequestPayload(
            Address("1.2.3.4", 1234),
            Address("2.2.3.4", 2234),
            Address("3.2.3.4", 3234),
            true,
            connectionType,
            1
        )
        val serialized = payload.serialize()
        assertEquals("0102030404d20202030408ba030203040ca2810001", serialized.toHex())
        val deserialized = IntroductionRequestPayload.deserialize(serialized)
        assertEquals(connectionType, deserialized.connectionType)
    }
}
