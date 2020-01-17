package nl.tudelft.ipv8.messaging.payload

import nl.tudelft.ipv8.util.toHex
import org.junit.Assert.assertEquals
import org.junit.Test

class BinMemberAuthenticationPayloadTest {
    @Test
    fun serialize() {
        val payload = BinMemberAuthenticationPayload(
            "aaa".toByteArray(Charsets.US_ASCII)
        )
        val serialized = payload.serialize()
        assertEquals("0003616161", serialized.toHex())
    }

    @Test
    fun deserialize() {
        val publicKey = "aaa".toByteArray(Charsets.US_ASCII)
        val payload = BinMemberAuthenticationPayload(publicKey)
        val serialized = payload.serialize()
        val (deserialized, size) = BinMemberAuthenticationPayload.deserialize(serialized)
        assertEquals(5, size)
        assertEquals(publicKey.toHex(), deserialized.publicKey.toHex())
    }
}
