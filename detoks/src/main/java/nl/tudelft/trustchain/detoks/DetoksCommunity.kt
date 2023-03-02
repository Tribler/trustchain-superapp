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

enum class MessageType {
    LIKE
}

class DetoksCommunity (settings: TrustChainSettings,
                       database: TrustChainStore,
                       crawler: TrustChainCrawler = TrustChainCrawler()
) : TrustChainCommunity(settings, database, crawler)  {
    // TODO: generate random service id for our community
    override val serviceId = "02313685c1912a141289f8248fc8db5899c5df5a"
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
        // TODO: change broadcast to subset of peers?
        for (peer in getPeers()) {
            // TODO: change test to the liker's public key
            val packet = serializePacket(MessageType.LIKE.ordinal, Like("test", vid))
            send(peer.address, packet)
        }
    }

    init {
        messageHandlers[MessageType.LIKE.ordinal] = ::onMessage
    }

    private fun onMessage(packet: Packet) {
        val (peer, payload) = packet.getAuthPayload(Like.Deserializer)
        // Because peers can relay the message, peer != liker in all cases
        Log.d("DeToks", payload.liker + " liked: " + payload.video)
    }
}

// also add torrent name and video creator
class Like(val liker: String, val video: String) : Serializable {
    override fun serialize(): ByteArray {
        return liker.toByteArray() + video.toByteArray()
    }

    companion object Deserializer : Deserializable<Like> {
        override fun deserialize(buffer: ByteArray, offset: Int): Pair<Like, Int> {
            val like = Like(buffer.toString(Charsets.UTF_8), )
            return Pair(like, buffer.size)
        }
    }
}
