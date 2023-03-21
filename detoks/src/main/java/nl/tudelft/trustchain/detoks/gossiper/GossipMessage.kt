package nl.tudelft.trustchain.detoks.gossiper

import nl.tudelft.ipv8.messaging.Deserializable
import nl.tudelft.ipv8.messaging.Serializable
import nl.tudelft.trustchain.detoks.DeToksCommunity

class GossipMessage(
    val id: Int,
    val data: List<Any>
    ) : Serializable {


    override fun serialize(): ByteArray {
        var msg = data
        if (id != DeToksCommunity.MESSAGE_TORRENT_ID) {
            msg = msg.map { (it as Pair<*, *>).first.toString() + "~" + it.second.toString() }
        }
        return msg.joinToString("," ).toByteArray()
    }

    companion object Deserializer : Deserializable<GossipMessage> {

        override fun deserialize(buffer: ByteArray, offset: Int): Pair<GossipMessage, Int> {
            val tempStr = String(buffer, offset,buffer.size - offset)
            val tempList = tempStr.split(",")

            if (!tempStr.contains("~")) {
                return Pair(GossipMessage(0, tempList), offset)
            }

            if(tempList.size == 1 && tempList[0] == "")
                return Pair(GossipMessage(0, listOf()), offset)

            val entries: List<Pair<String, Any>> = tempList.map {
                val strPair = it.split("~")
                Pair(strPair[0], strPair[1])
            }

            return Pair(GossipMessage(0, entries), offset)
        }
    }
}
