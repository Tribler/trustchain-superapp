package nl.tudelft.trustchain.detoks.gossiper.messages

import nl.tudelft.ipv8.messaging.Deserializable
import nl.tudelft.ipv8.messaging.Serializable

class NetworkSizeMessage(val entries: List<Pair<String, Double>>) : Serializable {

    override fun serialize(): ByteArray {
        val msg = entries.joinToString(",") { it.first + ":" + it.second.toString() }
        return msg.toByteArray()
    }

    companion object Deserializer : Deserializable<NetworkSizeMessage> {
        override fun deserialize(buffer: ByteArray, offset: Int): Pair<NetworkSizeMessage, Int> {

            val tempStr = String(buffer, offset,buffer.size - offset)

            //TODO: fix if nothing is sent
            val entries: List<Pair<String, Double>> = tempStr.split(",").map {
                val strPair = it.split(":")
                Pair(strPair[0], strPair[1].toDouble())
            }

            return Pair(NetworkSizeMessage(entries), offset)
        }
    }
}
