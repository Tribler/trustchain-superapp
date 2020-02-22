package nl.tudelft.ipv8.messaging.payload

import nl.tudelft.ipv8.Address
import nl.tudelft.ipv8.messaging.*

/**
 * The payload for an introduction-response message.
 *
 * When the associated request wanted advice the sender will also sent a puncture-request
 * message to either the lan_introduction_address or the wan_introduction_address
 * (depending on their positions).  The introduced node must sent a puncture message to the
 * receiver to punch a hole in its NAT.
 */
data class IntroductionResponsePayload(
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
     * The wan address of the sender. Nodes not in the same LAN should use this address to
     * communicate.
     */
    val sourceWanAddress: Address,

    /**
     * The lan address of the node that the sender advises the receiver to contact. This address
     * is zero when the associated request did not want advice.
     */
    val lanIntroductionAddress: Address,

    /**
     * The wan address of the node that the sender advises the receiver to contact. This address
     * is zero when the associated request did not want advice.
     */
    val wanIntroductionAddress: Address,

    /**
     * A unicode string indicating the connection type that the message creator has. Currently the
     * following values are supported: u"unknown", u"public", and u"symmetric-NAT".
     */
    val connectionType: ConnectionType,

    /**
     * A boolean indicating that the connection is tunneled and all messages send to the introduced
     * candidate require a ffffffff prefix.
     */
    val tunnel: Boolean,

    /**
     * A number that was given in the associated introduction-request.  This number allows to
     * distinguish between multiple introduction-response messages.
     */
    val identifier: Int
) : Serializable {
    override fun serialize(): ByteArray {
        return destinationAddress.serialize() +
                sourceLanAddress.serialize() +
                sourceWanAddress.serialize() +
                lanIntroductionAddress.serialize() +
                wanIntroductionAddress.serialize() +
                createConnectionByte(connectionType) +
                serializeUShort(identifier)
    }

    companion object Deserializer : Deserializable<IntroductionResponsePayload> {
        override fun deserialize(buffer: ByteArray, offset: Int): Pair<IntroductionResponsePayload, Int> {
            var localOffset = 0
            val (destinationAddress, _) = Address.deserialize(buffer, offset + localOffset)
            localOffset += Address.SERIALIZED_SIZE
            val (sourceLanAddress, _) = Address.deserialize(buffer, offset + localOffset)
            localOffset += Address.SERIALIZED_SIZE
            val (sourceWanAddress, _) = Address.deserialize(buffer, offset + localOffset)
            localOffset += Address.SERIALIZED_SIZE
            val (lanIntroductionAddress, _) = Address.deserialize(buffer, offset + localOffset)
            localOffset += Address.SERIALIZED_SIZE
            val (wanIntroductionAddress, _) = Address.deserialize(buffer, offset + localOffset)
            localOffset += Address.SERIALIZED_SIZE
            val (_, connectionType) = deserializeConnectionByte(buffer[offset + localOffset])
            localOffset++
            val identifier = deserializeUShort(buffer, offset + localOffset)
            localOffset += SERIALIZED_USHORT_SIZE
            val payload = IntroductionResponsePayload(
                destinationAddress,
                sourceLanAddress,
                sourceWanAddress,
                lanIntroductionAddress,
                wanIntroductionAddress,
                connectionType,
                false,
                identifier
            )
            return Pair(payload, localOffset)
        }
    }
}
