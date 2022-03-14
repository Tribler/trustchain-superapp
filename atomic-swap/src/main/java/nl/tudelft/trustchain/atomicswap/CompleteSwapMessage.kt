package nl.tudelft.trustchain.atomicswap

import nl.tudelft.ipv8.messaging.Deserializable
import nl.tudelft.ipv8.messaging.Serializable
import nl.tudelft.ipv8.util.toHex

data class CompleteSwapMessage(val offerId: String, val publicKey: String) : Serializable {
    override fun serialize(): ByteArray {
        val msgString = "$offerId;$publicKey;"
        println(msgString.toByteArray().toHex())
        return msgString.toByteArray()
    }

    companion object Deserializer : Deserializable<CompleteSwapMessage> {
        override fun deserialize(buffer: ByteArray, offset: Int): Pair<CompleteSwapMessage, Int> {
            val (offerId, publicKey) = buffer.drop(8).toByteArray().decodeToString()
                .split(";")
            return Pair(CompleteSwapMessage(offerId, publicKey), buffer.size)
        }
    }
}
