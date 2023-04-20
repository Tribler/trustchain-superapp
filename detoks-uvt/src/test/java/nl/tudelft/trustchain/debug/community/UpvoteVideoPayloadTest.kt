package nl.tudelft.trustchain.debug.community

import nl.tudelft.trustchain.detoks.community.UpvoteVideoPayload
import nl.tudelft.trustchain.detoks.token.UpvoteToken
import org.junit.Test

class UpvoteVideoPayloadTest {
    @Test
    fun serialize_deserialize_single_test() {
        // Create the initial list of UpvoteTokens
        val inputList: List<UpvoteToken> = listOf(
            UpvoteToken(1, "2023-04-01", "SomePublicKey", "SomeVideoID", "SeederID"),
        )

        // Serialize and deserialize the list
        val serializedList = UpvoteVideoPayload(inputList).serialize()
        val deserializedPayload = UpvoteVideoPayload.deserialize(serializedList, 0)
        val deserializedList = deserializedPayload.first.upvoteTokens

        // Check if the input list is the same as the deserialized list
        assert(inputList == deserializedList)
    }



    @Test
    fun serialize_deserialize_multiple_test() {
        // Create the initial list of UpvoteTokens
        val inputList: List<UpvoteToken> = listOf(
            UpvoteToken(1, "2023-04-01", "SomePublicKey", "SomeVideoID", "SeederID"),
            UpvoteToken(2, "2023-04-02", "SomePublicKey2", "SomeVideoTestID", "SeederIdentifier"),
            UpvoteToken(3, "2023-04-03", "SomePrivate?Key", "SomeDifferentVideoID", "SomePrivate?Key"),
            UpvoteToken(4, "2023-05-01", "TestingThisKey", "SomeVideoID4","SomePublicKey2"),
            UpvoteToken(5, "2022-04-01", "SomeOtherPublicKey", "SomeVideoOtherID", "SeederId"),
        )

        // Serialize and deserialize the list
        val serializedList = UpvoteVideoPayload(inputList).serialize()
        val deserializedPayload = UpvoteVideoPayload.deserialize(serializedList, 0)
        val deserializedList = deserializedPayload.first.upvoteTokens

        // Check if the input list is the same as the deserialized list
        assert(inputList == deserializedList)
    }

    @Test
    fun serialize_deserialize_differentLength_test() {
        // Create the initial list of UpvoteTokens
        val inputList: List<UpvoteToken> = listOf(
            UpvoteToken(1, "2023-04-01", "SomePublicKey", "SomeVideoID", "SeederID"),
            UpvoteToken(2, "2023-04-02", "SomePublicKey2", "SomeVideoTestID", "SeederIdentifier"),
            UpvoteToken(3, "2023-04-03", "SomePrivate?Key", "SomeDifferentVideoID", "SomePrivate?Key"),
        )

        // Serialize and deserialize the list
        val serializedList = UpvoteVideoPayload(inputList).serialize()
        val deserializedPayload = UpvoteVideoPayload.deserialize(serializedList, 0)
        val deserializedList = deserializedPayload.first.upvoteTokens

        // Check if the input list is the same as the deserialized list
        assert(inputList == deserializedList)
    }
}
