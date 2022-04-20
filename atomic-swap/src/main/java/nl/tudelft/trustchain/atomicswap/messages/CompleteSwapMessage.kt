package nl.tudelft.trustchain.atomicswap.messages

import nl.tudelft.ipv8.messaging.Deserializable
import nl.tudelft.ipv8.messaging.Serializable
import nl.tudelft.ipv8.util.toHex

data class CompleteSwapMessage(val offerId: String, val txId: String) : Serializable {
    override fun serialize(): ByteArray {
        val msgString = "$offerId;$txId;"
        println(msgString.toByteArray().toHex())
        return msgString.toByteArray()
    }

    companion object Deserializer : Deserializable<CompleteSwapMessage> {
        override fun deserialize(buffer: ByteArray, offset: Int): Pair<CompleteSwapMessage, Int> {
            val (offerId, txId) = buffer.drop(offset).toByteArray().decodeToString()
                .split(";")
            return Pair(CompleteSwapMessage(offerId, txId), buffer.size)
        }
    }
}
