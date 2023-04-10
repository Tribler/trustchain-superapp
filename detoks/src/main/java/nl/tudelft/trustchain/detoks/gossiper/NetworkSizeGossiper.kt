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
import kotlin.random.Random.Default.nextDouble

class NetworkSizeGossiper(
    override val delay: Long,
    override val peers: Int,
    private val leaders: Int
) : Gossiper() {

    private var firstCycle = true

    override fun startGossip(coroutineScope: CoroutineScope) {
        coroutineScope.launch {
            while (coroutineScope.isActive) {
                if (firstCycle) {
                    firstCycle = false
                    delay(System.currentTimeMillis() % delay)
                }
                gossip()
                delay(delay)
            }
        }
    }

    override suspend fun gossip() {
        val deToksCommunity = IPv8Android.getInstance().getOverlay<DeToksCommunity>()!!

        if (leaderEstimates.isNotEmpty())
            networkSizeEstimate = (1 / leaderEstimates.minOfOrNull { it.second }!!).toInt()

        awaitingResponse.clear()

        val chanceLeader = leaders / (networkSizeEstimate.toDouble())
        Log.d(DeToksCommunity.LOGGING_TAG, "Chance to become leader: $chanceLeader, leaders: $leaders, networksize estimate: $networkSizeEstimate")
        leaderEstimates = if (nextDouble() < chanceLeader)
            listOf(Pair(deToksCommunity.myPeer.mid, 1.0))
        else listOf()

        val randomPeers = pickRandomN(deToksCommunity.getPeers(), peers)
        randomPeers.forEach {
            awaitingResponse.add(it.mid)
            deToksCommunity.gossipWith(
                it,
                NetworkSizeMessage(leaderEstimates),
                DeToksCommunity.MESSAGE_NETWORK_SIZE_ID
            )
        }
    }

    companion object {
        var networkSizeEstimate = 1

        private var leaderEstimates = listOf<Pair<String, Double>>()

        private val awaitingResponse = mutableListOf<String>()

        /**
         * Applies counting algorithm to determine network size.
         */
        fun receivedResponse(msg: NetworkSizeMessage, peer: Peer) {
            if (!awaitingResponse.contains(peer.mid)) {
                val deToksCommunity = IPv8Android.getInstance().getOverlay<DeToksCommunity>()!!
                deToksCommunity.gossipWith(
                    peer,
                    NetworkSizeMessage(leaderEstimates),
                    DeToksCommunity.MESSAGE_NETWORK_SIZE_ID
                )
            }

            val myKeys = leaderEstimates.map { it.first }
            val otherKeys = msg.data.map { it.first }

            val myUnique = leaderEstimates
                .filter { !otherKeys.contains(it.first) }
                .map { Pair(it.first, it.second / 2) }

            val otherUnique = msg.data
                .filter { !myKeys.contains(it.first) }
                .map { Pair(it.first, it.second / 2) }

            val shared = leaderEstimates
                .filter { otherKeys.contains(it.first) }
                .map {
                    Pair(
                        it.first, it.second + (
                            msg.data.find { it1 -> it1.first == it.first }?.second ?: 0.0
                            ) / 2
                    )
                }

            leaderEstimates = listOf(myUnique, otherUnique, shared).flatten()
        }
    }
}

class NetworkSizeMessage(data: List<Pair<String, Double>>) : GossipMessage<Double>(data) {
    companion object Deserializer : Deserializable<NetworkSizeMessage> {
        override fun deserialize(buffer: ByteArray, offset: Int): Pair<NetworkSizeMessage, Int> {
            val msg = deserializeMessage(buffer, offset){
                return@deserializeMessage Pair(it.getString(0), it.getDouble(1))
            }
            return Pair(NetworkSizeMessage(msg.first), msg.second)
        }
    }
}
