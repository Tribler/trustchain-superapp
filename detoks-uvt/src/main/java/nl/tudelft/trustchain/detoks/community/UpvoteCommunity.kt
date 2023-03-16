package nl.tudelft.trustchain.detoks.community

import android.content.Context
import nl.tudelft.ipv8.Community
import nl.tudelft.ipv8.Overlay
import nl.tudelft.ipv8.Peer
import nl.tudelft.ipv8.messaging.Packet
import mu.KotlinLogging
import nl.tudelft.ipv8.attestation.trustchain.*
import nl.tudelft.ipv8.attestation.trustchain.store.TrustChainStore
import nl.tudelft.ipv8.util.toHex

private val logger = KotlinLogging.logger {}

object UpvoteTrustchainConstants {
    const val GIVE_HEART_TOKEN = "give_heart_token_block"
}
class UpvoteCommunity(
    context: Context,
    settings: TrustChainSettings,
    database: TrustChainStore,
    crawler: TrustChainCrawler = TrustChainCrawler()
) : TrustChainCommunity(settings, database, crawler){
    /**
     * serviceId is a randomly generated hex string with length 40
     */
    override val serviceId = "ee6ce7b5ad81eef11f4fcff335229ba169c03aeb"
    val context = context
    init {
        messageHandlers[MessageID.HEART_TOKEN] = ::onHeartTokenPacket
        this.registerBlockSigner(UpvoteTrustchainConstants.GIVE_HEART_TOKEN, object : BlockSigner {
            override fun onSignatureRequest(block: TrustChainBlock) {
                logger.debug { "This peer with member ID: ${myPeer.mid} received \n" +
                    "the link_public key from the proposal block is ${block.linkPublicKey.toHex()}\n" +
                    "mypeer's public key is: ${myPeer.publicKey.keyToBin().toHex()}" +
                    "a proposal block from  peer with public key: ${block.publicKey.toHex()}, contents of the transaction:\n" +
                    "the video ID is: ${block.transaction["videoID"]},\n" +
                    "the heartTokenGivenBy: ${block.transaction["heartTokenGivenBy"]},\n" +
                    "the heartTokenGivenTo: ${block.transaction["heartTokenGivenTo"]},\n" +
                    "Agreement Block created and sent\n"}
                this@UpvoteCommunity.createAgreementBlock(block, block.transaction)
            }
        })
    }

    object MessageID {
        const val HEART_TOKEN = 1
    }

    private fun onHeartTokenPacket(packet: Packet){
        val (peer, payload) = packet.getAuthPayload(HeartTokenPayload.Deserializer)
        onHeartToken(peer, payload)
    }

    private fun onHeartToken(peer: Peer, payload: HeartTokenPayload) {
        // do something with the payload
        logger.debug { "-> received heart token with id: ${payload.id}  and token: ${payload.token} from peer with member id: ${peer.mid}" }
    }

    /**
     * Selects a random Peer from the list of known Peers
     * @returns A random Peer or null if there are no known Peers
     */
    private fun pickRandomPeer(): Peer? {
        val peers = getPeers()
        if (peers.isEmpty()) return null
        return peers.random()
    }

    /**
     * Sends a HeartToken to a random Peer
     * When a message is sent, a proposal block is created
     * //TODO: only make proposal block if the user did not like the video yet, if the user already like the video,
     * //TODO: it shouldn't be possible to like again / create a proposal again
     *
     * //TODO: maybe it should be the poster of the video that creates proposal blocks and
     * //todo: the user that likes it creates an agreement block
     */
    fun sendHeartToken(id: String, token: String): String {
        val payload = HeartTokenPayload(id, token)

        val packet = serializePacket(
            MessageID.HEART_TOKEN,
            payload
        )

        val peer = pickRandomPeer()

        if (peer != null) {
            val message = "You/Peer with member id: ${myPeer.mid} is sending a heart token to peer with peer id: ${peer.mid}"
            logger.debug { message }
            send(peer, packet)

//            val transaction = mapOf("videoID" to "TODO: REPLACE THIS WITH ACTUAL VIDEO ID",
//            "heartTokenGivenBy" to myPeer.publicKey.keyToBin().toHex(),
//            "heartTokenGivenTo" to peer.publicKey.keyToBin().toHex())
//            val block = this.createProposalBlock(UpvoteTrustchainConstants.GIVE_HEART_TOKEN, transaction, peer.publicKey.keyToBin());
//            logger.debug { "proposal block created:" +
//                "peer with public key: ${block.publicKey.toHex()} has created a proposal block,\n" +
//                "contents of block:\n" +
//                "video ID: ${block.transaction["videoID"]}\n" +
//                "peer with public key: ${block.transaction["heartTokenGivenBy"]} gave a Heart token to \n" +
//                "proposal block needs an agreement block from peer with public key: ${block.transaction["heartTokenGivenTo"]}" }
            return message
        }

        return "No peer found"
    }

    class Factory(
        private val context: Context,
        private val settings: TrustChainSettings,
        private val database: TrustChainStore,
        private val crawler: TrustChainCrawler = TrustChainCrawler()
    ) : Overlay.Factory<UpvoteCommunity>(UpvoteCommunity::class.java) {
        override fun create(): UpvoteCommunity {
            return UpvoteCommunity(context, settings, database, crawler)
        }
    }

}
