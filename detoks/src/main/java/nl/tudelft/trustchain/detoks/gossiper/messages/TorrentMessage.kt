package nl.tudelft.trustchain.detoks.gossiper.messages

import nl.tudelft.ipv8.messaging.Deserializable
import nl.tudelft.ipv8.messaging.Serializable
import org.json.JSONArray


class TorrentMessage(val magnets: List<String>) : Serializable {

    override fun serialize(): ByteArray {
        return JSONArray(magnets).toString().toByteArray()
    }

    companion object Deserializer : Deserializable<TorrentMessage> {
        override fun deserialize(buffer: ByteArray, offset: Int): Pair<TorrentMessage, Int> {
            val tempStr = String(buffer, offset,buffer.size - offset)
            val jsonEnd = tempStr.indexOf(']') + 1

            val jsonArray = JSONArray(tempStr.substring(0, jsonEnd))
            val magnets = mutableListOf<String>()

            for (i in 0 until jsonArray.length()) {
                magnets.add(jsonArray.getString(i))
            }

            return Pair(TorrentMessage(magnets), jsonEnd + offset)
        }
    }
}
