package nl.tudelft.trustchain.atomicswap.messages

import nl.tudelft.ipv8.messaging.Deserializable
import nl.tudelft.ipv8.messaging.Serializable
import nl.tudelft.ipv8.util.toHex

data class AcceptMessage(val offerId: String, val btcPubKey: String, val ethAddress: String) : Serializable {
    override fun serialize(): ByteArray {
        val msgString = "$offerId;$btcPubKey;$ethAddress;"
        println(msgString.toByteArray().toHex())
        return msgString.toByteArray()
    }

    companion object Deserializer : Deserializable<AcceptMessage> {
        override fun deserialize(buffer: ByteArray, offset: Int): Pair<AcceptMessage, Int> {
            val (offerId, btcKey, ethAddress) = buffer.drop(offset).toByteArray().decodeToString().split(";")
            return Pair(AcceptMessage(offerId, btcKey, ethAddress), buffer.size)
        }
    }
}
