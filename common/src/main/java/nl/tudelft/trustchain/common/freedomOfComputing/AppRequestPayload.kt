package nl.tudelft.trustchain.common.freedomOfComputing

import nl.tudelft.ipv8.messaging.Deserializable
import nl.tudelft.ipv8.messaging.Serializable
import nl.tudelft.ipv8.messaging.deserializeVarLen
import nl.tudelft.ipv8.messaging.serializeVarLen

data class AppRequestPayload(
    val appTorrentInfoHash: String
) : Serializable {
    override fun serialize(): ByteArray {
        return serializeVarLen(appTorrentInfoHash.toByteArray())
    }

    companion object Deserializer : Deserializable<AppRequestPayload> {
        override fun deserialize(buffer: ByteArray, offset: Int): Pair<AppRequestPayload, Int> {
            var localOffset = offset
            val (appTorrentInfoHash, idSize) = deserializeVarLen(buffer, localOffset)
            localOffset += idSize
            return Pair(
                AppRequestPayload(appTorrentInfoHash.toString(Charsets.UTF_8)),
                localOffset - offset
            )
        }
    }
}
