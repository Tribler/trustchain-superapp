package nl.tudelft.trustchain.detoks

import android.util.Log
import nl.tudelft.ipv8.messaging.Deserializable
import nl.tudelft.ipv8.messaging.Serializable
import org.json.JSONArray


class TorrentMessage(val magnet: String) : Serializable {

    override fun serialize(): ByteArray {
        return magnet.toByteArray()
    }

    companion object Deserializer : Deserializable<TorrentMessage> {
        override fun deserialize(buffer: ByteArray, offset: Int): Pair<TorrentMessage, Int> {
            val tempStr = String(buffer, offset,buffer.size - offset)

            return Pair(TorrentMessage(tempStr), offset)
        }
    }


}
