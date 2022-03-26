package nl.tudelft.trustchain.common.freedomOfComputing

import nl.tudelft.ipv8.messaging.Deserializable
import nl.tudelft.ipv8.messaging.Serializable
import nl.tudelft.ipv8.messaging.deserializeVarLen
import nl.tudelft.ipv8.messaging.serializeVarLen

class AppPayload(
    val appTorrentInfoHash: String,
    val appName: String,
    val data: ByteArray
) : Serializable {
    override fun serialize(): ByteArray {
        return serializeVarLen(appTorrentInfoHash.toByteArray()) +
            serializeVarLen(appName.toByteArray()) +
            serializeVarLen(data)
    }

    companion object Deserializer : Deserializable<AppPayload> {
        override fun deserialize(buffer: ByteArray, offset: Int): Pair<AppPayload, Int> {
            var localOffset = offset
            val (appTorrentInfoHash, hashSize) = deserializeVarLen(buffer, localOffset)
            localOffset += hashSize
            val (appName, nameSize) = deserializeVarLen(buffer, localOffset)
            localOffset += nameSize
            val (data, dataSize) = deserializeVarLen(buffer, localOffset)
            localOffset += dataSize
            return Pair(
                AppPayload(
                    appTorrentInfoHash.toString(Charsets.UTF_8),
                    appName.toString(Charsets.UTF_8),
                    data
                ),
                localOffset - offset
            )
        }
    }
}
