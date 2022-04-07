package nl.tudelft.trustchain.frost

import nl.tudelft.ipv8.messaging.*

import nl.tudelft.ipv8.messaging.Deserializable
import nl.tudelft.ipv8.messaging.Serializable
import nl.tudelft.ipv8.messaging.deserializeVarLen
import nl.tudelft.ipv8.messaging.serializeVarLen

class KeyPacketMessage constructor(
    val keyShare: ByteArray
) : Serializable {
    override fun serialize(): ByteArray {
        return serializeVarLen(keyShare)
    }

    companion object Deserializer : Deserializable<KeyPacketMessage> {
        override fun deserialize(buffer: ByteArray, offset: Int): Pair<KeyPacketMessage, Int> {
            var localOffset = offset
            val (keyShare, size) = deserializeVarLen(buffer, localOffset)
            localOffset += size
            return Pair(
                KeyPacketMessage(keyShare),
                localOffset - offset
            )
        }
    }
}
