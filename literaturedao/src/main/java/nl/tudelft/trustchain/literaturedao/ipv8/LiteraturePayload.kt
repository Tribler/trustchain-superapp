package nl.tudelft.trustchain.literaturedao.ipv8

import nl.tudelft.ipv8.messaging.Deserializable
import nl.tudelft.ipv8.messaging.Serializable
import nl.tudelft.ipv8.messaging.deserializeVarLen
import nl.tudelft.ipv8.messaging.serializeVarLen

class LiteraturePayload(
    val literatureTorrentInfoHash: String,
    val literatureName: String,
    val data: ByteArray
) : Serializable {
    override fun serialize(): ByteArray {
        return serializeVarLen(literatureTorrentInfoHash.toByteArray()) +
            serializeVarLen(literatureName.toByteArray()) +
            serializeVarLen(data)
    }

    companion object Deserializer : Deserializable<LiteraturePayload> {
        override fun deserialize(buffer: ByteArray, offset: Int): Pair<LiteraturePayload, Int> {
            var localOffset = offset
            val (appTorrentInfoHash, hashSize) = deserializeVarLen(buffer, localOffset)
            localOffset += hashSize
            val (appName, nameSize) = deserializeVarLen(buffer, localOffset)
            localOffset += nameSize
            val (data, dataSize) = deserializeVarLen(buffer, localOffset)
            localOffset += dataSize
            return Pair(
                LiteraturePayload(
                    appTorrentInfoHash.toString(Charsets.UTF_8),
                    appName.toString(Charsets.UTF_8),
                    data
                ),
                localOffset - offset
            )
        }
    }
}
