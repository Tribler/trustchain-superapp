package nl.tudelft.trustchain.datavault.community

import nl.tudelft.ipv8.messaging.Deserializable
import nl.tudelft.ipv8.messaging.Serializable
import nl.tudelft.ipv8.messaging.deserializeVarLen
import nl.tudelft.ipv8.messaging.serializeVarLen

class VaultFileRequestFailedPayload(val message: String) : Serializable {
    override fun serialize(): ByteArray {
        return serializeVarLen(message.toByteArray())
    }

    companion object Deserializer : Deserializable<VaultFileRequestFailedPayload> {
        override fun deserialize(buffer: ByteArray, offset: Int): Pair<VaultFileRequestFailedPayload, Int> {
            val (messageBytes, _) = deserializeVarLen(buffer, offset)

            return Pair(VaultFileRequestFailedPayload(String(messageBytes)), 0)
        }

    }
}
