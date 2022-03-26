package nl.tudelft.trustchain.atomicswap.messages

import nl.tudelft.ipv8.messaging.Deserializable
import nl.tudelft.ipv8.messaging.Serializable
import nl.tudelft.ipv8.util.toHex

data class InitiateMessage(val offerId: String, val hash:String, val txId: String, val publicKey: String) : Serializable {
    override fun serialize(): ByteArray {
        val msgString = "$offerId;$hash;$txId;$publicKey;"
        println(msgString.toByteArray().toHex())
        return msgString.toByteArray()
    }

    companion object Deserializer : Deserializable<InitiateMessage> {
        override fun deserialize(buffer: ByteArray, offset: Int): Pair<InitiateMessage, Int> {
            val (offerId, hash, txId, publicKey) = buffer.drop(offset).toByteArray().decodeToString()
                .split(";")
            return Pair(InitiateMessage(offerId, hash, txId, publicKey), buffer.size)
        }
    }
}
