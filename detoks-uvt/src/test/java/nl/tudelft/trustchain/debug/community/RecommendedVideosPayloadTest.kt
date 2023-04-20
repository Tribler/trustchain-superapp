package nl.tudelft.trustchain.debug.community

import nl.tudelft.trustchain.detoks.community.RecommendedVideosPayload
import org.junit.Test

class RecommendedVideosPayloadTest {
    @Test
    fun serialize_deserialize_test() {
        val inputList: List<String> = listOf("VideoID_1", "VideoID_2", "VideoID_3")
        val serializedList = RecommendedVideosPayload(inputList).serialize()
        val deserializedPayload = RecommendedVideosPayload.deserialize(serializedList, 0)
        val deserializedList = deserializedPayload.first.recommendations
        assert(deserializedList.size == 3)
        assert(deserializedList.contains("VideoID_1"))
        assert(deserializedList.contains("VideoID_2"))
        assert(deserializedList.contains("VideoID_3"))
    }
}
