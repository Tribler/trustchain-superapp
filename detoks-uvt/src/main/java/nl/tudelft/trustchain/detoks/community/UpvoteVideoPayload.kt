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

                val (idBytes, idSize) = deserializeVarLen(buffer, localOffset)
                val id = idBytes.toString(Charsets.UTF_8).toInt()
                localOffset += idSize

                val (dateBytes, dateSize) = deserializeVarLen(buffer, localOffset)
                val date = dateBytes.toString(Charsets.UTF_8)
                localOffset += dateSize

                val (keyMinterBytes, publicKeySize) = deserializeVarLen(buffer, localOffset)
                val keyMinter = keyMinterBytes.toString(Charsets.UTF_8)
                localOffset += publicKeySize

                val (videoIdBytes, videoIdSize) = deserializeVarLen(buffer, localOffset)
                val videoId = videoIdBytes.toString(Charsets.UTF_8)
                localOffset += videoIdSize

                val (keySeederBytes, seederSize) = deserializeVarLen(buffer, localOffset)
                val keySeeder = keySeederBytes.toString(Charsets.UTF_8)
                localOffset += seederSize

                val upvoteToken = UpvoteToken(id, date, keyMinter, videoId, keySeeder)
                upvoteTokenList.add(upvoteToken)
            }

            return Pair(
                UpvoteVideoPayload(upvoteTokenList),
                localOffset - offset
            )
        }
    }
}

