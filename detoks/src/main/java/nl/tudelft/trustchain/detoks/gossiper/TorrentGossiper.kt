package nl.tudelft.trustchain.detoks.gossiper

import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import nl.tudelft.ipv8.android.IPv8Android
import nl.tudelft.ipv8.util.random
import nl.tudelft.trustchain.detoks.DeToksCommunity
import nl.tudelft.trustchain.detoks.TorrentManager
import nl.tudelft.trustchain.detoks.gossiper.messages.TorrentMessage

class TorrentGossiper(
    override val delay: Long,
    override val blocks: Int,
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
        val randomPeer = pickRandomPeer(deToksCommunity) ?: return

        val handlers = TorrentManager.getInstance(context).getListOfTorrents()
        val max = if (handlers.size < blocks) handlers.size else blocks
        val randomMagnets = handlers.random(max).map { it.makeMagnetUri() }

        if(randomMagnets.isNotEmpty())
            deToksCommunity.gossipWith(randomPeer, TorrentMessage(randomMagnets), DeToksCommunity.MESSAGE_TORRENT_ID)
    }
}
