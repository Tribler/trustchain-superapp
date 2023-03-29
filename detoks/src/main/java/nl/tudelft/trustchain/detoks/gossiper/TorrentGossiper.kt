package nl.tudelft.trustchain.detoks.gossiper

import android.content.Context
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
        val randomPeers = pickRandomN(deToksCommunity.getPeers(), peers)
        val handlers = TorrentManager.getInstance(context).getListOfTorrents()
        if (randomPeers.isEmpty() || handlers.isEmpty()) return

        val max = if (handlers.size < blocks) handlers.size else blocks
        val randomMagnets = handlers.random(max).map { it.makeMagnetUri() }

        if(randomMagnets.isNotEmpty())
            randomPeers.forEach {
                val data = randomMagnets.map {it2 -> Pair(
                    it2,
                    TorrentManager.getInstance(context).profile
                        .torrents[MagnetLink.hashFromMagnet(it2)]!!.hopCount + 1)} // increment hop
                deToksCommunity.gossipWith(
                    it,
                    TorrentMessage(data),
                    DeToksCommunity.MESSAGE_TORRENT_ID
                )
            }
    }
}

class TorrentMessage(data: List<Pair<String, Int>>) : GossipMessage<Int>(data) {
    companion object Deserializer : Deserializable<TorrentMessage> {
        override fun deserialize(buffer: ByteArray, offset: Int): Pair<TorrentMessage, Int> {
            val msg = deserializeMessage(buffer, offset){
                return@deserializeMessage Pair(it.getString(0), it.getInt(1))
            }
            return Pair(TorrentMessage(msg.first), msg.second)
        }
    }
}
