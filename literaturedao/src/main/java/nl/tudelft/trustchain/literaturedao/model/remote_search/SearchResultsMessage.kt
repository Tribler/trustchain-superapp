package nl.tudelft.trustchain.literaturedao.model.remote_search

import com.google.gson.Gson
import nl.tudelft.ipv8.messaging.Deserializable

data class SearchResultsMessage(val results: SearchResultList) :
    nl.tudelft.ipv8.messaging.Serializable {
    override fun serialize(): ByteArray {
        return Gson().toJson(results).toByteArray()
    }

    companion object Deserializer : Deserializable<SearchResultsMessage> {
        override fun deserialize(buffer: ByteArray, offset: Int): Pair<SearchResultsMessage, Int> {
            var toReturn =
                Gson().fromJson(buffer.toString(Charsets.UTF_8), SearchResultList::class.java)
            return Pair(SearchResultsMessage(toReturn), buffer.size)
        }
    }
}
