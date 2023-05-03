package nl.tudelft.trustchain.detoks.gossiper

import android.content.Context
import android.util.Log
import com.frostwire.jlibtorrent.Sha1Hash
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import nl.tudelft.ipv8.android.IPv8Android
import nl.tudelft.ipv8.messaging.Deserializable
import nl.tudelft.ipv8.util.random
import nl.tudelft.trustchain.detoks.DeToksCommunity
import nl.tudelft.trustchain.detoks.MagnetLink
import nl.tudelft.trustchain.detoks.TorrentManager
import kotlin.math.max

class TorrentGossiper(
    override val delay: Long,
    override val peers: Int,
    private val blocks: Int,
    val context: Context
) : Gossiper() {

    override fun startGossip(coroutineScope: CoroutineScope) {
        coroutineScope.launch {
            while (coroutineScope.isActive) {
                gossip()
                delay(delay)
            }
        }
    }

    override suspend fun gossip() {
        val deToksCommunity = IPv8Android.getInstance().getOverlay<DeToksCommunity>()!!
        val torrentManager = TorrentManager.getInstance(context)
        val handlers = torrentManager.getListOfTorrents()
        val randomPeers = pickRandomN(deToksCommunity.getPeers(), peers)
        if (randomPeers.isEmpty() || handlers.isEmpty()) return

        val max = if (handlers.size < blocks) handlers.size else blocks
        val randomMagnets = handlers.random(max).map { it.makeMagnetUri() }

        randomMagnets.forEach { magnet ->
            val torrentInfo = torrentManager.getInfoFromMagnet(magnet)?:return@forEach
            for (it in 0 until torrentInfo.numFiles()) {
                val fileName = torrentInfo.files().fileName(it)
                if (!fileName.endsWith(".mp4")) continue

                // Create the unique video key and compose the profile contents into a list of data
                val key = torrentManager.createKey(MagnetLink.hashFromMagnet(magnet), it)
                val entry = torrentManager.profile.addProfile(key)
                val data = listOf(
                    Pair("Key", key),
                    Pair("WatchTime", entry.watchTime.toString()),
                    Pair("Likes", entry.likes.toString()),
                    Pair("Duration", entry.duration.toString()),
                    Pair("UploadDate",entry.uploadDate.toString()),
                    Pair("HopCount", (entry.hopCount + 1).toString())
                )

                // Gossip the profile of the video with peers
                randomPeers.forEach { peer ->
                    deToksCommunity.gossipWith(
                        peer,
                        TorrentMessage(data),
                        DeToksCommunity.MESSAGE_TORRENT_ID
                    )
                }
            }
        }
    }

    companion object {
        fun receivedResponse(data: List<Pair<String, String>>, context: Context) {
            if(data[0].first != "Key") {
                Log.d(DeToksCommunity.LOGGING_TAG, "Torrent message without key received")
                return
            }

            // Retrieve the key and add the torrent to the torrent manager
            val key = data[0].second
            val torrentManager = TorrentManager.getInstance(context)
            val hash = MagnetLink.hashFromMagnet(key)
            torrentManager.addTorrent(Sha1Hash(hash), key)

            // Handle the list of metrics and update the profile accordingly
            val profile = torrentManager.profile
            val entry = profile.addProfile(key)
            data.drop(0).forEach {
                when(it.first) {
                    "WatchTime" -> profile.updateEntryWatchTime(key, it.second.toLong(), false)
                    "Likes" -> profile.updateEntryLikes(key, it.second.toInt(), false)
                    "Duration" -> entry.duration = max(entry.duration, it.second.toLong())
                    "UploadDate" -> entry.uploadDate = max(entry.uploadDate, it.second.toLong())
                    "HopCount" -> profile.updateEntryHopCount(key, it.second.toInt())
                    else -> Log.d(DeToksCommunity.LOGGING_TAG, "Received data in torrent message that was not recognized")
                }
            }
        }
    }
}

class TorrentMessage(data: List<Pair<String, String>>) : GossipMessage<String>(data) {
    companion object Deserializer : Deserializable<TorrentMessage> {
        override fun deserialize(buffer: ByteArray, offset: Int): Pair<TorrentMessage, Int> {
            val msg = deserializeMessage(buffer, offset){
                return@deserializeMessage Pair(it.getString(0), it.getString(1))
            }
            return Pair(TorrentMessage(msg.first), msg.second)
        }
    }
}
