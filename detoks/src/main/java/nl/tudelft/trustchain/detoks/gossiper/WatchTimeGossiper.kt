package nl.tudelft.trustchain.detoks.gossiper

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import nl.tudelft.ipv8.android.IPv8Android
import nl.tudelft.trustchain.detoks.DeToksCommunity
import nl.tudelft.trustchain.detoks.gossiper.messages.WatchTimeMessage

class WatchTimeGossiper(
    override val delay: Long,
    override val blocks: Int
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

        if(deToksCommunity.watchTimeQueue.isEmpty()) return

        val maxEntries = if (deToksCommunity.watchTimeQueue.size < blocks) deToksCommunity.watchTimeQueue.size else blocks
        val watchTimes = deToksCommunity.watchTimeQueue.subList(0, maxEntries)

        deToksCommunity.getPeers().forEach {
            deToksCommunity.gossipWith(it, WatchTimeMessage(watchTimes), DeToksCommunity.MESSAGE_WATCH_TIME_ID)
        }
        watchTimes.clear()
    }
}
