package nl.tudelft.trustchain.common.freedomOfComputing

import nl.tudelft.ipv8.messaging.Deserializable
import nl.tudelft.ipv8.messaging.Serializable
import nl.tudelft.ipv8.messaging.deserializeVarLen
import nl.tudelft.ipv8.messaging.serializeVarLen

data class AppRequestPayload(
    val appTorrentInfoHash: String,
    val uuid: String // Created because EVA doesn't allow retransmits
) : Serializable {
    override fun serialize(): ByteArray {
        return serializeVarLen(appTorrentInfoHash.toByteArray()) +
            serializeVarLen(uuid.toByteArray())
    }

    companion object Deserializer : Deserializable<AppRequestPayload> {
        override fun deserialize(buffer: ByteArray, offset: Int): Pair<AppRequestPayload, Int> {
            var localOffset = offset
            val (appTorrentInfoHash, idSize) = deserializeVarLen(buffer, localOffset)
            localOffset += idSize
            val (uuid, uuidSize) = deserializeVarLen(buffer, localOffset)
            localOffset += uuidSize
            return Pair(
                AppRequestPayload(
                    appTorrentInfoHash.toString(Charsets.UTF_8),
                    uuid.toString(Charsets.UTF_8)
                ),
                localOffset - offset
            )
        }
    }
}
