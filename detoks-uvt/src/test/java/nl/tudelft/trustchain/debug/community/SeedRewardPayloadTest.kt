package nl.tudelft.trustchain.debug.community

import nl.tudelft.trustchain.detoks.community.SeedRewardPayload
import nl.tudelft.trustchain.detoks.token.UpvoteToken
import org.junit.Assert.assertEquals
import org.junit.Test

class SeedRewardPayloadTest {

    @Test
    fun testSerializeAndDeserialize() {
        val blockHash = byteArrayOf(0x01, 0x02, 0x03)
        val upvoteToken1 = UpvoteToken(1, "2023-04-15", "12345678", "1", "0987654321")
        val upvoteToken2 = UpvoteToken(2, "2023-04-15", "12345678910", "2", "09876543211")
        val upvoteTokens = listOf(upvoteToken1, upvoteToken2)

        val payload = SeedRewardPayload(blockHash, upvoteTokens)

        val serializedPayload = payload.serialize()

        val (deserializedPayload, _) = SeedRewardPayload.Deserializer.deserialize(serializedPayload, 0)

        assertEquals(payload.blockHash.contentToString(), deserializedPayload.blockHash.contentToString())
        assertEquals(payload.upvoteTokens.size, deserializedPayload.upvoteTokens.size)
        for (i in payload.upvoteTokens.indices) {
            assertEquals(payload.upvoteTokens[i].tokenID, deserializedPayload.upvoteTokens[i].tokenID)
            assertEquals(payload.upvoteTokens[i].date, deserializedPayload.upvoteTokens[i].date)
            assertEquals(payload.upvoteTokens[i].publicKeyMinter, deserializedPayload.upvoteTokens[i].publicKeyMinter)
            assertEquals(payload.upvoteTokens[i].videoID, deserializedPayload.upvoteTokens[i].videoID)
            assertEquals(payload.upvoteTokens[i].publicKeySeeder, deserializedPayload.upvoteTokens[i].publicKeySeeder)
        }
    }
}
