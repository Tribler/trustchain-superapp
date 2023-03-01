package nl.tudelft.trustchain.detoks
import android.util.Log
import nl.tudelft.ipv8.IPv4Address
import nl.tudelft.ipv8.Community
import nl.tudelft.ipv8.Overlay
import nl.tudelft.ipv8.Peer
import nl.tudelft.ipv8.android.IPv8Android
import nl.tudelft.ipv8.attestation.trustchain.*
import nl.tudelft.ipv8.attestation.trustchain.store.TrustChainStore
import nl.tudelft.ipv8.messaging.Deserializable
import nl.tudelft.ipv8.messaging.Packet
import nl.tudelft.ipv8.messaging.Serializable
import nl.tudelft.ipv8.messaging.payload.IntroductionResponsePayload
import java.util.*


class DetoksCommunity (settings: TrustChainSettings,
                       database: TrustChainStore,
                       crawler: TrustChainCrawler = TrustChainCrawler()
) : TrustChainCommunity(settings, database, crawler)  {
    // TODO: WHAT THIS?
    override val serviceId = "02313685c1912a141289f8248fc8db5899c5df5a"
    private val LIKE_ID = 1
    val discoveredAddressesContacted: MutableMap<IPv4Address, Date> = mutableMapOf()
    val lastTrackerResponses = mutableMapOf<IPv4Address, Date>()


    class Factory(
        private val settings: TrustChainSettings,
        private val database: TrustChainStore,
        private val crawler: TrustChainCrawler = TrustChainCrawler()
    ) : Overlay.Factory<DetoksCommunity>(DetoksCommunity::class.java) {
        override fun create(): DetoksCommunity {
            return DetoksCommunity(settings, database, crawler)
        }
    }

    override fun walkTo(address: IPv4Address) {
        super.walkTo(address)

        discoveredAddressesContacted[address] = Date()
    }

    // Retrieve the DeToks community

    private fun getDetoksComm(): DetoksCommunity {
        return IPv8Android.getInstance().getOverlay<DetoksCommunity>()
            ?: throw IllegalStateException("DeToks community is not configured")
    }

    override fun onIntroductionResponse(peer: Peer, payload: IntroductionResponsePayload) {
        super.onIntroductionResponse(peer, payload)

        if (peer.address in DEFAULT_ADDRESSES) {
            lastTrackerResponses[peer.address] = Date()
        }
    }

    fun broadcastLike(vid: String) {
        Log.d("DeToks", "Liking: $vid")
        for (peer in getPeers()) {
            val packet = serializePacket(LIKE_ID, Like(vid))
            send(peer.address, packet)

        }
    }

    init {
        messageHandlers[LIKE_ID] = ::onMessage
    }

    private fun onMessage(packet: Packet) {
        val (peer, payload) = packet.getAuthPayload(Like.Deserializer)
        Log.d("DeToks", peer.mid + " liked: " + payload.message)
    }
}

class Like(val message: String) : Serializable {
    override fun serialize(): ByteArray {
        return message.toByteArray()
    }

    companion object Deserializer : Deserializable<Like> {
        override fun deserialize(buffer: ByteArray, offset: Int): Pair<Like, Int> {
            return Pair(Like(buffer.toString(Charsets.UTF_8)), buffer.size)
        }
    }
}
