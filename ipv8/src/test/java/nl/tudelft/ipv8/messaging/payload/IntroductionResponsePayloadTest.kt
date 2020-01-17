package nl.tudelft.ipv8.messaging.payload

import nl.tudelft.ipv8.Address
import nl.tudelft.ipv8.util.toHex
import org.junit.Assert.assertEquals
import org.junit.Test

class IntroductionResponsePayloadTest {
    @Test
    fun serialize() {
        val payload = IntroductionResponsePayload(
            Address("1.2.3.4", 1234),
            Address("2.2.3.4", 2234),
            Address("3.2.3.4", 3234),
            Address("4.2.3.4", 4234),
            Address("5.2.3.4", 5234),
            ConnectionType.UNKNOWN,
            false,
            2
        )
        val serialized = payload.serialize()
        assertEquals("0102030404d20202030408ba030203040ca204020304108a050203041472000002", serialized.toHex())
    }

    @Test
    fun deserialize() {
        val destinationAddress = Address("1.2.3.4", 1234)
        val sourceLanAddress = Address("2.2.3.4", 2234)
        val sourceWanAddress = Address("3.2.3.4", 3234)
        val lanIntroductionAddress = Address("4.2.3.4", 4234)
        val wanIntroductionAddress = Address("5.2.3.4", 5234)
        val identifier = 2
        val payload = IntroductionResponsePayload(
            destinationAddress,
            sourceLanAddress,
            sourceWanAddress,
            lanIntroductionAddress,
            wanIntroductionAddress,
            ConnectionType.UNKNOWN,
            false,
            identifier
        )
        val serialized = payload.serialize()
        val (deserialized, size) = IntroductionResponsePayload.deserialize(serialized)
        assertEquals(destinationAddress, deserialized.destinationAddress)
        assertEquals(sourceLanAddress, deserialized.sourceLanAddress)
        assertEquals(sourceWanAddress, deserialized.sourceWanAddress)
        assertEquals(lanIntroductionAddress, deserialized.lanIntroductionAddress)
        assertEquals(wanIntroductionAddress, deserialized.wanIntroductionAddress)
        assertEquals(false, deserialized.tunnel)
        assertEquals(ConnectionType.UNKNOWN, deserialized.connectionType)
        assertEquals(identifier, deserialized.identifier)
        assertEquals(size, serialized.size)
    }
}
