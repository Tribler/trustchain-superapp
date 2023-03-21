package nl.tudelft.trustchain.detoks.gossiper

import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import nl.tudelft.ipv8.Peer
import nl.tudelft.ipv8.android.IPv8Android
import nl.tudelft.trustchain.detoks.DeToksCommunity

class BootGossiper(
    override val delay: Long,
    override val peers: Int
) : Gossiper() {

    var maxLoops = (30000/delay).toInt()

    override fun startGossip(coroutineScope: CoroutineScope) {
        coroutineScope.launch {
            while (coroutineScope.isActive && running) {
                if (maxLoops <= 1)
                    running = false
                gossip()
                maxLoops -= 1
                delay(delay)
            }
        }
    }

    override suspend fun gossip() {
        val deToksCommunity = IPv8Android.getInstance().getOverlay<DeToksCommunity>()!!
        val randomPeers = pickRandomN(deToksCommunity.getPeers(), peers)

        randomPeers.forEach {
            deToksCommunity.gossipWith(
                it,
                GossipMessage(DeToksCommunity.MESSAGE_BOOT_REQUEST, listOf()),
                DeToksCommunity.MESSAGE_BOOT_REQUEST
            )
        }
    }

    companion object {
        var running = true

        fun receivedRequest(peer: Peer) {
            val data = listOf<Pair<String, Any>>(
                Pair("NetworkSize", NetworkSizeGossiper.networkSizeEstimate)
            )
            val deToksCommunity = IPv8Android.getInstance().getOverlay<DeToksCommunity>()!!
            deToksCommunity.gossipWith(
                peer,
                GossipMessage(DeToksCommunity.MESSAGE_BOOT_RESPONSE, data),
                DeToksCommunity.MESSAGE_BOOT_RESPONSE
            )
        }

        fun receivedResponse(data: List<Any>) {
            data.forEach {
                when((it as Pair<*,*>).first as String) {
                    "NetworkSize" ->
                        NetworkSizeGossiper.networkSizeEstimate = (it.second as String).toInt()
                    else ->
                        Log.d(DeToksCommunity.LOGGING_TAG, "Received data in boot message that wasn't recognized")
                }
            }
            running = false
            Log.d(DeToksCommunity.LOGGING_TAG, "Received boot response, shutting down gossiper")
        }
    }
}
