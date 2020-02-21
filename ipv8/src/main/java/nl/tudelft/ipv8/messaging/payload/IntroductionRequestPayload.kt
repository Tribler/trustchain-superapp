package nl.tudelft.ipv8.messaging.payload

import nl.tudelft.ipv8.Address
import nl.tudelft.ipv8.messaging.*

/**
 * The payload for an introduction-request message.
 */
data class IntroductionRequestPayload(
    /**
     * The address of the receiver. Effectively this should be the wan address that others can
     * use to contact the receiver.
     */
    val destinationAddress: Address,

    /**
     * The lan address of the sender. Nodes in the same LAN should use this address to communicate.
     */
    val sourceLanAddress: Address,

    /**
     * The wan address of the sender. Nodes not in the same LAN should use this address
     * to communicate.
     */
    val sourceWanAddress: Address,

    /**
     * When True the receiver will introduce the sender to a new node.  This introduction will be
     * facilitated by the receiver sending a puncture-request to the new node.
     */
    val advice: Boolean,

    /**
     * Indicating the connection type that the message creator has.
     */
    val connectionType: ConnectionType,

    /**
     * A number that must be given in the associated introduction-response. This number allows to
     * distinguish between multiple introduction-response messages.
     */
    val identifier: Int
) : Serializable {
    override fun serialize(): ByteArray {
        return destinationAddress.serialize() +
                sourceLanAddress.serialize() +
                sourceWanAddress.serialize() +
                createConnectionByte(connectionType, advice) +
                serializeUShort(identifier)
    }

    companion object Deserializer : Deserializable<IntroductionRequestPayload> {
        override fun deserialize(buffer: ByteArray, offset: Int): Pair<IntroductionRequestPayload, Int> {
            var localOffset = 0
            val (destinationAddress, _) = Address.deserialize(buffer, offset + localOffset)
            localOffset += Address.SERIALIZED_SIZE
            val (sourceLanAddress, _) = Address.deserialize(buffer, offset + localOffset)
            localOffset += Address.SERIALIZED_SIZE
            val (sourceWanAddress, _) = Address.deserialize(buffer, offset + localOffset)
            localOffset += Address.SERIALIZED_SIZE
            val (advice, connectionType) = deserializeConnectionByte(buffer[offset + localOffset])
            localOffset++
            val identifier = deserializeUShort(buffer, offset + localOffset)
            localOffset += SERIALIZED_USHORT_SIZE
            val payload = IntroductionRequestPayload(
                destinationAddress,
                sourceLanAddress,
                sourceWanAddress,
                advice,
                connectionType,
                identifier
            )
            return Pair(payload, localOffset)
        }
    }
}
