package nl.tudelft.ipv8.messaging.payload

import nl.tudelft.ipv8.Address
import nl.tudelft.ipv8.messaging.Deserializable
import nl.tudelft.ipv8.messaging.Serializable
import nl.tudelft.ipv8.messaging.deserializeUShort
import nl.tudelft.ipv8.messaging.serializeUShort
import kotlin.experimental.and

class IntroductionRequestPayload(
    val destinationAddress: Address,
    val sourceLanAddress: Address,
    val sourceWanAddress: Address,
    val advice: Boolean,
    val connectionType: ConnectionType,
    val identifier: Int
) : Serializable {
    override fun serialize(): ByteArray {
        return destinationAddress.serialize() +
                sourceLanAddress.serialize() +
                sourceWanAddress.serialize() +
                createConnectionByte() +
                serializeUShort(identifier)
    }

    private fun createConnectionByte(): Byte {
        var connectionByte: UByte = 0x00u
        if (connectionType.encoding.first) {
            connectionByte = connectionByte or 0x80.toUByte()
        }
        if (connectionType.encoding.second) {
            connectionByte = connectionByte or 0x40.toUByte()
        }
        if (advice) {
            connectionByte = connectionByte or 0x01.toUByte()
        }
        return connectionByte.toByte()
    }

    companion object : Deserializable<IntroductionRequestPayload> {
        override fun deserialize(buffer: ByteArray, offset: Int): IntroductionRequestPayload {
            var off = offset
            val destinationAddress = Address.deserialize(buffer, off)
            off += Address.SERIALIZED_SIZE
            val sourceLanAddress = Address.deserialize(buffer, off)
            off += Address.SERIALIZED_SIZE
            val sourceWanAddress = Address.deserialize(buffer, off)
            off += Address.SERIALIZED_SIZE
            val (advice, connectionType) = deserializeConnectionByte(buffer[off])
            off++
            val identifier = deserializeUShort(buffer, off)
            return IntroductionRequestPayload(
                destinationAddress,
                sourceLanAddress,
                sourceWanAddress,
                advice,
                connectionType,
                identifier
            )
        }

        private fun deserializeConnectionByte(byte: Byte): Pair<Boolean, ConnectionType> {
            val advice = (byte and 0x01) == 0x01.toByte()
            val bit0 = (byte.toUByte() and 0x80.toUByte()) != 0x00.toUByte()
            val bit1 = (byte.toUByte() and 0x40.toUByte()) != 0x00.toUByte()
            return Pair(advice, ConnectionType.decode(bit0, bit1))
        }
    }
}
