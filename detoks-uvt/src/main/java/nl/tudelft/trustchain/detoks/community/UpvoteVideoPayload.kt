package nl.tudelft.trustchain.detoks.community

import mu.KotlinLogging
import nl.tudelft.ipv8.messaging.*
import nl.tudelft.trustchain.detoks.token.UpvoteToken

private val logger = KotlinLogging.logger {}

class UpvoteVideoPayload constructor(val upvoteTokens: List<UpvoteToken>) : Serializable {


    override fun serialize(): ByteArray {

        var payload: ByteArray = ByteArray(0)

        for(upvoteToken: UpvoteToken in upvoteTokens) {
            payload += upvoteToken.toByteArray()
        }

        return payload
    }

    companion object Deserializer : Deserializable<UpvoteVideoPayload> {
        override fun deserialize(buffer: ByteArray, offset: Int): Pair<UpvoteVideoPayload, Int> {
            var localOffset = offset
            val upvoteTokenList: ArrayList<UpvoteToken> = ArrayList()

            while (localOffset < buffer.size) {

                val (token_id, token_idSize) = deserializeVarLen(buffer, localOffset)
                localOffset += token_idSize

                val (date, dateSize) = deserializeVarLen(buffer, localOffset)
                localOffset += dateSize

                val (public_key, public_keySize) = deserializeVarLen(buffer, localOffset)
                localOffset += public_keySize

                val (video_id, video_idSize) = deserializeVarLen(buffer, localOffset)
                localOffset += video_idSize

                val upvoteToken = UpvoteToken(token_id.toString(Charsets.UTF_8).toInt(), date.toString(Charsets.UTF_8), public_key.toString(Charsets.UTF_8), video_id.toString(Charsets.UTF_8))
                upvoteTokenList.add(upvoteToken)
            }

            return Pair(
                UpvoteVideoPayload(upvoteTokenList),
                localOffset - offset
            )
        }
    }
}

