package nl.tudelft.ipv8.messaging.payload

import nl.tudelft.ipv8.Address
import nl.tudelft.ipv8.messaging.*

/**
 * The payload for a puncture-request payload.
 */
data class PunctureRequestPayload(
    /**
     * The lan address of the node that the sender wants us to contact. This contact attempt should
     * punch a hole in our NAT to allow the node to connect to us.
     */
    val lanWalkerAddress: Address,

    /**
     * The wan address of the node that the sender wants us to contact. This contact attempt should
     * punch a hole in our NAT to allow the node to connect to us.
     */
    val wanWalkerAddress: Address,

    /**
     * A number that was given in the associated introduction-request. This number allows to
     * distinguish between multiple introduction-response messages.
     */
    val identifier: Int
) : Serializable {
    override fun serialize(): ByteArray {
        return lanWalkerAddress.serialize() +
                wanWalkerAddress.serialize() +
                serializeUShort(identifier)
    }

    companion object Deserializer : Deserializable<PunctureRequestPayload> {
        override fun deserialize(buffer: ByteArray, offset: Int): Pair<PunctureRequestPayload, Int> {
            var localOffset = 0
            val (lanWalkerAddress, _) = Address.deserialize(buffer, offset + localOffset)
            localOffset += Address.SERIALIZED_SIZE
            val (wanWalkerAddress, _) = Address.deserialize(buffer, offset + localOffset)
            localOffset += Address.SERIALIZED_SIZE
            val identifier = deserializeUShort(buffer, offset + localOffset)
            localOffset += SERIALIZED_USHORT_SIZE
            val payload = PunctureRequestPayload(lanWalkerAddress, wanWalkerAddress, identifier)
            return Pair(payload, localOffset)
        }
    }
}
