package nl.tudelft.trustchain.atomicswap.messages

import nl.tudelft.ipv8.messaging.Deserializable
import nl.tudelft.ipv8.messaging.Serializable
import nl.tudelft.ipv8.util.toHex

class RemoveTradeMessage(val offerId: String) : Serializable {
    override fun serialize(): ByteArray {
        val msgString = "$offerId;"
        return msgString.toByteArray()
    }

    companion object Deserializer : Deserializable<RemoveTradeMessage> {
        override fun deserialize(buffer: ByteArray, offset: Int): Pair<RemoveTradeMessage, Int> {
            val (offerId) = buffer.drop(offset).toByteArray().decodeToString().split(";")
            return Pair(RemoveTradeMessage(offerId), buffer.size)
        }
    }
}
