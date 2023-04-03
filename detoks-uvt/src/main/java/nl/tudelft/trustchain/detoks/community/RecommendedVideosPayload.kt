package nl.tudelft.trustchain.detoks.community

import nl.tudelft.ipv8.messaging.Deserializable
import nl.tudelft.ipv8.messaging.Serializable
import nl.tudelft.ipv8.messaging.deserializeVarLen
import nl.tudelft.ipv8.messaging.serializeVarLen

class RecommendedVideosPayload constructor(val recommendations: List<String>): Serializable {

    override fun serialize(): ByteArray {

        var payload: ByteArray = ByteArray(0)

        for(recommendation: String in recommendations) {
            payload += serializeVarLen(recommendation.toByteArray())
        }

        return payload
    }

    companion object Deserializer : Deserializable<RecommendedVideosPayload> {
        override fun deserialize(
            buffer: ByteArray,
            offset: Int
        ): Pair<RecommendedVideosPayload, Int> {
            var localOffset = offset
            val recommendations = ArrayList<String>()

            while (localOffset < buffer.size) {
                val (recommendation, recommendationSize) = deserializeVarLen(buffer, localOffset)
                localOffset += recommendationSize
                recommendations.add(recommendation.toString(Charsets.UTF_8))
            }

            return Pair(
                RecommendedVideosPayload(recommendations),
                localOffset - offset
            )
        }
    }
}
