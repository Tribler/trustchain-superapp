package nl.tudelft.trustchain.detoks.community

import nl.tudelft.ipv8.messaging.*

class UpvoteTokenPayload constructor(val token_id: String, val date: String, val public_key_minter: String, val video_id: String) : Serializable {
    override fun serialize(): ByteArray {
        return serializeVarLen(token_id.toByteArray()) + serializeVarLen(date.toByteArray()) + serializeVarLen(public_key_minter.toByteArray()) + serializeVarLen(video_id.toByteArray())
    }

    companion object Deserializer : Deserializable<UpvoteTokenPayload> {
        override fun deserialize(buffer: ByteArray, offset: Int): Pair<UpvoteTokenPayload, Int> {
            var localOffset = offset
            val (token_id, token_idSize) = deserializeVarLen(buffer, localOffset)
            localOffset += token_idSize
            val (date, dateSize) = deserializeVarLen(buffer, localOffset)
            localOffset += dateSize
            val (public_key, public_keySize) = deserializeVarLen(buffer, localOffset)
            localOffset += public_keySize
            val (video_id, video_idSize) = deserializeVarLen(buffer, localOffset)
            localOffset += video_idSize

            return Pair(
                UpvoteTokenPayload(token_id.toString(Charsets.UTF_8), date.toString(Charsets.UTF_8), public_key.toString(Charsets.UTF_8), video_id.toString(Charsets.UTF_8)),
                localOffset - offset
            )
        }
    }
}

