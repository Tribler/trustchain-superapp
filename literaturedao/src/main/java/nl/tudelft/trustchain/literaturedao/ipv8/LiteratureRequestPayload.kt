package nl.tudelft.trustchain.literaturedao.ipv8

import nl.tudelft.ipv8.messaging.Deserializable
import nl.tudelft.ipv8.messaging.Serializable
import nl.tudelft.ipv8.messaging.deserializeVarLen
import nl.tudelft.ipv8.messaging.serializeVarLen

data class LiteratureRequestPayload(
    val literatureTorrentInfoHash: String
) : Serializable {
    override fun serialize(): ByteArray {
        return serializeVarLen(literatureTorrentInfoHash.toByteArray())
    }

    companion object Deserializer : Deserializable<LiteratureRequestPayload> {
        override fun deserialize(
            buffer: ByteArray,
            offset: Int
        ): Pair<LiteratureRequestPayload, Int> {
            var localOffset = offset
            val (appTorrentInfoHash, idSize) = deserializeVarLen(buffer, localOffset)
            localOffset += idSize
            return Pair(
                LiteratureRequestPayload(appTorrentInfoHash.toString(Charsets.UTF_8)),
                localOffset - offset
            )
        }
    }
}
