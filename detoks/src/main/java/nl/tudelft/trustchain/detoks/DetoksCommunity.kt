package nl.tudelft.trustchain.detoks

import java.util.*
import android.util.Log

import nl.tudelft.ipv8.IPv4Address
import nl.tudelft.ipv8.Community
import nl.tudelft.ipv8.Overlay
import nl.tudelft.ipv8.Peer
import nl.tudelft.ipv8.android.IPv8Android
import nl.tudelft.ipv8.attestation.trustchain.*
import nl.tudelft.ipv8.attestation.trustchain.store.TrustChainStore
import nl.tudelft.ipv8.messaging.Packet
import nl.tudelft.ipv8.messaging.payload.IntroductionResponsePayload
import nl.tudelft.trustchain.detoks.Like

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

    fun broadcastLike(vid: String, torrent: String, creator: String) {
        Log.d("DeToks", "Liking: $vid")

        // TODO: get own public key

        var creator1 = creator
        for (peer in getPeers()) {
            creator1 = peer.toString();
        }
        print(getPeers().size)
        val my_public_key = myPeer.publicKey.toString()
        val like = Like(my_public_key, vid, torrent, creator1)
        val map = mapOf("like" to like)
        Log.d("DeToks", map.toString())
        createProposalBlock("like_block", map, creator1.toByteArray())
//        // TODO: change broadcast to subset of peers?
//        for (peer in getPeers()) {
//            val packet = serializePacket(MessageType.LIKE.ordinal, Like("my public key", vid))
//            send(peer.address, packet)
//        }
    }

    init {
        messageHandlers[MessageType.LIKE.ordinal] = ::onMessage
        registerBlockSigner("like_block", object : BlockSigner {
            override fun onSignatureRequest(block: TrustChainBlock) {
                createAgreementBlock(block, mapOf<Any?, Any?>())
            }
        })
        addListener("like_block", object : BlockListener {
            override fun onBlockReceived(block: TrustChainBlock) {
                Log.d("TrustChainDemo", "onBlockReceived: ${block.blockId} ${block.transaction}")
            }
        })
    }

    private fun onMessage(packet: Packet) {
        val (_, payload) = packet.getAuthPayload(Like.Deserializer)
        // Because peers can relay the message, peer != liker in all cases
        Log.d("DeToks", payload.liker + " liked: " + payload.video + " Peer "/* + peer.address.toString()*/)
        // TODO: propagate message

    }
}
