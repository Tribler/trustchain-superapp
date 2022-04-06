package nl.tudelft.trustchain.atomicswap.messages

import nl.tudelft.ipv8.messaging.Deserializable
import nl.tudelft.ipv8.messaging.Serializable
import nl.tudelft.ipv8.util.toHex

class RemoveTradeMessage(val offerId: String) : Serializable {
    override fun serialize(): ByteArray {
        return offerId.toByteArray()
    }

    companion object Deserializer : Deserializable<RemoveTradeMessage> {
        override fun deserialize(buffer: ByteArray, offset: Int): Pair<RemoveTradeMessage, Int> {
            return Pair(RemoveTradeMessage(buffer.toString(Charsets.UTF_8)), buffer.size)
        }
    }
}
