package nl.tudelft.trustchain.debug.community

import nl.tudelft.trustchain.detoks.community.UpvoteTokenPayload
import org.junit.Assert.assertEquals
import org.junit.Test

class UpvoteTokenPayloadTest {

    @Test
    fun testSerializeAndDeserialize() {
        val tokenId = "1"
        val date = "2023-04-15"
        val publicKeyMinter = "12345678910111213"
        val videoId = "1"

        val payload = UpvoteTokenPayload(tokenId, date, publicKeyMinter, videoId)

        val serializedPayload = payload.serialize()

        val (deserializedPayload, _) = UpvoteTokenPayload.Deserializer.deserialize(serializedPayload, 0)

        assertEquals(payload.token_id, deserializedPayload.token_id)
        assertEquals(payload.date, deserializedPayload.date)
        assertEquals(payload.public_key_minter, deserializedPayload.public_key_minter)
        assertEquals(payload.video_id, deserializedPayload.video_id)
    }
}
