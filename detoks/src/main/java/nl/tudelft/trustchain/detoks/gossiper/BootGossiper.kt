package nl.tudelft.trustchain.detoks.gossiper

import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import nl.tudelft.ipv8.Peer
import nl.tudelft.ipv8.android.IPv8Android
import nl.tudelft.ipv8.messaging.Deserializable
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
                BootMessage(listOf()),
                DeToksCommunity.MESSAGE_BOOT_REQUEST
            )
        }
    }

    companion object {
        var running = true

        fun receivedRequest(peer: Peer) {
            val data = listOf(
                Pair("NetworkSize", NetworkSizeGossiper.networkSizeEstimate.toString())
            )
            val deToksCommunity = IPv8Android.getInstance().getOverlay<DeToksCommunity>()!!
            deToksCommunity.gossipWith(
                peer,
                BootMessage(data),
                DeToksCommunity.MESSAGE_BOOT_RESPONSE
            )
        }

        fun receivedResponse(data: List<Pair<String, String>>) {
            data.forEach {
                when(it.first) {
                    "NetworkSize" ->
                        NetworkSizeGossiper.networkSizeEstimate = it.second.toInt()
                    else ->
                        Log.d(DeToksCommunity.LOGGING_TAG, "Received data in boot message that wasn't recognized")
                }
            }
            running = false
            Log.d(DeToksCommunity.LOGGING_TAG, "Received boot response, shutting down gossiper")
        }
    }
}

class BootMessage(data: List<Pair<String, String>>) : GossipMessage<String>(data) {
    companion object Deserializer : Deserializable<BootMessage> {
        override fun deserialize(buffer: ByteArray, offset: Int): Pair<BootMessage, Int> {
            val msg = deserializeMessage(buffer, offset){
                return@deserializeMessage Pair(it.getString(0), it.getString(1))
            }
            return Pair(BootMessage(msg.first), msg.second)
        }
    }
}
