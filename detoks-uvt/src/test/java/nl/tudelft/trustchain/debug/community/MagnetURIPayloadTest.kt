package nl.tudelft.trustchain.debug.community

import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.assertNotNull
import junit.framework.TestCase.assertTrue
import nl.tudelft.trustchain.detoks.community.MagnetURIPayload
import org.junit.Test

class MagnetURIPayloadTest {

    @Test
    fun testSerialize() {
        val magnetUri = "magnet:?xt=urn:btih:abcdef&dn=test"
        val proposalTokenHash = "12345"
        val payload = MagnetURIPayload(magnetUri, proposalTokenHash)

        val serialized = payload.serialize()

        assertNotNull(serialized)
        assertTrue(serialized.isNotEmpty())
    }

    @Test
    fun testDeserialize() {
        val magnetUri = "magnet:?xt=urn:btih:abcdef&dn=test"
        val proposalTokenHash = "12345"
        val payload = MagnetURIPayload(magnetUri, proposalTokenHash)
        val serialized = payload.serialize()

        val (deserialized, bytesRead) = MagnetURIPayload.Deserializer.deserialize(serialized, 0)

        assertNotNull(deserialized)
        assertEquals(magnetUri, deserialized.magnet_uri)
        assertEquals(proposalTokenHash, deserialized.proposal_token_hash)
        assertEquals(serialized.size, bytesRead)
    }
}
