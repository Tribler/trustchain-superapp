package nl.tudelft.trustchain.detoks.gossiper

import android.net.Network
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import nl.tudelft.ipv8.IPv8
import nl.tudelft.ipv8.Peer
import nl.tudelft.ipv8.android.IPv8Android
import nl.tudelft.trustchain.detoks.DeToksCommunity
import kotlin.system.exitProcess

class BootGossiper(
    override val delay: Long,
    override val peers: Int
) : Gossiper() {

    var maxLoops = (30/delay).toInt()

    override fun startGossip(coroutineScope: CoroutineScope) {
        coroutineScope.launch {
            while (coroutineScope.isActive) {
                if (maxLoops < 0)
                    exitProcess(0)
                gossip()
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
        fun sendResponse(peer: Peer) {
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

        }
    }
}
