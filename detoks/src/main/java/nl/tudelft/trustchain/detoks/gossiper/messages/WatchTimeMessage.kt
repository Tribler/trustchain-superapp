package nl.tudelft.trustchain.detoks.gossiper.messages

import nl.tudelft.ipv8.messaging.Deserializable
import nl.tudelft.ipv8.messaging.Serializable

class WatchTimeMessage(val entries: List<Pair<String, Long>>) : Serializable {

    override fun serialize(): ByteArray {
        val msg = entries.map { it.first + ":" + it.second.toString() }
            .joinToString(",")
        return msg.toByteArray()
    }

    companion object Deserializer : Deserializable<WatchTimeMessage> {
        override fun deserialize(buffer: ByteArray, offset: Int): Pair<WatchTimeMessage, Int> {

            val tempStr = String(buffer, offset,buffer.size - offset)

            val entries: List<Pair<String, Long>> = tempStr.split(",").map {
                val strPair = it.split(":")
                Pair(strPair[0], strPair[1].toLong())
            }

            return Pair(WatchTimeMessage(entries), offset)
        }
    }
}
