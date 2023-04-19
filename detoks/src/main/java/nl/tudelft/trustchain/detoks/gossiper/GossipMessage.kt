package nl.tudelft.trustchain.detoks.gossiper

import nl.tudelft.ipv8.messaging.Serializable
import org.json.JSONArray

abstract class GossipMessage<T>(
    val data: List<Pair<String, T>>
    ) : Serializable {


    override fun serialize(): ByteArray {
        return JSONArray(data.map { JSONArray(arrayOf(it.first, it.second)) }.toTypedArray())
            .toString().toByteArray()
    }

    companion object Deserializer {

        fun <T>deserializeMessage(buffer: ByteArray, offset: Int, func: (JSONArray) -> T): Pair<List<T>, Int> {
            val tempStr = String(buffer, offset,buffer.size - offset)
            val json = JSONArray(tempStr)
            val entries = mutableListOf<T>()

            for (i in 0 until json.length())
                entries.add(func(json.getJSONArray(i)))

            return Pair(entries, offset)
        }
    }
}
