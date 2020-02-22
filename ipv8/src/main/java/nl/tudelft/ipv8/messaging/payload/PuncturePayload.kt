package nl.tudelft.ipv8.messaging.payload

import nl.tudelft.ipv8.Address
import nl.tudelft.ipv8.messaging.*

/**
 * The payload for a puncture message.
 */
data class PuncturePayload(
    /**
     * The lan address of the sender. Nodes in the same LAN should use this address to communicate.
     */
    val sourceLanAddress: Address,

    /**
     * The wan address of the sender. Nodes not in the same LAN should use this address to
     * communicate.
     */
    val sourceWanAddress: Address,

    /**
     * A number that was given in the associated introduction-request. This number allows to
     * distinguish between multiple introduction-response messages.
     */
    val identifier: Int
) : Serializable {
    override fun serialize(): ByteArray {
        return sourceLanAddress.serialize() +
                sourceWanAddress.serialize() +
                serializeUShort(identifier)
    }

    companion object Deserializer : Deserializable<PuncturePayload> {
        override fun deserialize(buffer: ByteArray, offset: Int): Pair<PuncturePayload, Int> {
            var localOffset = 0
            val (sourceLanAddress, _) = Address.deserialize(buffer, offset + localOffset)
            localOffset += Address.SERIALIZED_SIZE
            val (sourceWanAddress, _) = Address.deserialize(buffer, offset + localOffset)
            localOffset += Address.SERIALIZED_SIZE
            val identifier = deserializeUShort(buffer, offset + localOffset)
            localOffset += SERIALIZED_USHORT_SIZE
            val payload = PuncturePayload(sourceLanAddress, sourceWanAddress, identifier)
            return Pair(payload, localOffset)
        }
    }
}
