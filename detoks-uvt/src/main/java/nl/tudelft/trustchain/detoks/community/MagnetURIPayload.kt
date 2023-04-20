package nl.tudelft.trustchain.detoks.community

import mu.KotlinLogging
import nl.tudelft.ipv8.messaging.*

private val logger = KotlinLogging.logger {}

class MagnetURIPayload constructor(val magnet_uri: String, val proposal_token_hash: String) : Serializable {

    override fun serialize(): ByteArray {
        return serializeVarLen(magnet_uri.toByteArray()) + serializeVarLen(proposal_token_hash.toByteArray())
    }

    companion object Deserializer : Deserializable<MagnetURIPayload> {
        override fun deserialize(buffer: ByteArray, offset: Int): Pair<MagnetURIPayload, Int> {
            var localOffset = offset
            val (magnet_uri, magnet_uriSize) = deserializeVarLen(buffer, localOffset)
            localOffset += magnet_uriSize
            val (proposal_token_hash, proposal_token_hashSize) = deserializeVarLen(buffer, localOffset)
            localOffset += proposal_token_hashSize

            return Pair(
                MagnetURIPayload(magnet_uri.toString(Charsets.UTF_8), proposal_token_hash.toString(Charsets.UTF_8)),
                localOffset - offset
            )
        }
    }
}

