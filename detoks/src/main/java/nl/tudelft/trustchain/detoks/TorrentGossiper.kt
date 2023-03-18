package nl.tudelft.trustchain.detoks

import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import nl.tudelft.ipv8.android.IPv8Android
import nl.tudelft.ipv8.util.random

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

        val randomPeer = pickRandomPeer(deToksCommunity)
        val handlers = TorrentManager.getInstance(context).getListOfTorrents()
        val max = if (handlers.size < blocks) handlers.size else blocks
        val randomMagnets = handlers.random(max).map { it.makeMagnetUri() }

        if(randomPeer != null && randomMagnets.isNotEmpty())
            deToksCommunity.gossipWith(randomPeer, TorrentMessage(randomMagnets))

        delay(delay)
    }
}
