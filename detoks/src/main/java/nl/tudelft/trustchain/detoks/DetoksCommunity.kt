package nl.tudelft.trustchain.detoks

import java.util.*
import android.util.Log
import nl.tudelft.ipv8.IPv4Address
import nl.tudelft.ipv8.Overlay
import nl.tudelft.ipv8.Peer
import nl.tudelft.ipv8.android.IPv8Android
import nl.tudelft.ipv8.attestation.trustchain.*
import nl.tudelft.ipv8.attestation.trustchain.store.TrustChainStore
import nl.tudelft.ipv8.messaging.payload.IntroductionResponsePayload
import kotlin.collections.ArrayList
import kotlin.math.sign

const val LIKE_BLOCK: String = "like_block"

class DetoksCommunity (settings: TrustChainSettings,
                       database: TrustChainStore,
                       crawler: TrustChainCrawler = TrustChainCrawler()
) : TrustChainCommunity(settings, database, crawler)  {
    override val serviceId = "02333685c1912a141289f8248fc8db5899c5df5a"
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

        // TODO: fix this
//        val peer = (0 until getPeers().size).random()
//        val peerKey = getPeers()[peer].key.keyToBin()
        val timestamp = System.currentTimeMillis().toString()
        val like = Like(myPeer.publicKey.toString(), vid, torrent, creator,timestamp)
        createProposalBlock(LIKE_BLOCK, like.toMap(), myPeer.publicKey.keyToBin())
        // createProposalBlock(LIKE_BLOCK, like.toMap(), peerKey)
        Log.d("DeToks", "$like")
    }

    fun getLikes(vid: String, torrent: String): List<TrustChainBlock> {
        return database.getBlocksWithType(LIKE_BLOCK).filter {
            it.transaction["video"] == vid && it.transaction["torrent"] == torrent
        }
    }

    fun getBlocksByAuthor(author: String): List<TrustChainBlock> {
        return database.getBlocksWithType(LIKE_BLOCK).sortedWith(compareByDescending{(it.transaction["timestamp"] as String).toLong()}).filter {
            it.transaction["author"] == author
        }
    }

    fun userLikedVideo(vid: String, torrent: String, liker: String): Boolean {
        return getLikes(vid, torrent).filter { it.transaction["liker"] == liker }.isNotEmpty()
    }

    fun getPostedVideos(author: String): List<Pair<String, Int>> {
        // Create Key data class so we can group by two fields (torrent and video)
        data class Key(val video: String, val torrent: String)
        fun TrustChainBlock.toKey() = Key(transaction["video"] as String, transaction["torrent"] as String)
        var likes = getBlocksByAuthor(author).groupBy { it.toKey() }
        // no need to sort here as getblocksbyauthor already sorts
        return likes.entries.map {
            Pair(it.key.video, it.value.size)
        }
    }

    fun listOfLikedVideosAndTorrents(person: String): List<Pair<String,String>> {
        var iterator = database.getBlocksWithType(LIKE_BLOCK).sortedWith(compareByDescending{(it.transaction["timestamp"] as String).toLong()}).filter {
            it.transaction["liker"] == person
        }.listIterator()
        var likedVideos = ArrayList<Pair<String,String>>()
        while(iterator.hasNext()) {
            val block = iterator.next()
            likedVideos.add(Pair(block.transaction.get("video") as String, block.transaction.get("torrent") as String))
        }
        return likedVideos;
    }

    init {
        registerBlockSigner(LIKE_BLOCK, object : BlockSigner {
            override fun onSignatureRequest(block: TrustChainBlock) {
                // TODO: Something is wrong here, loads of empty transaction blocks on the client
                createAgreementBlock(block, block.transaction)
            }
        })
        addListener(LIKE_BLOCK, object : BlockListener {
            override fun onBlockReceived(block: TrustChainBlock) {
                Log.d("Detoks", "onBlockReceived: ${block.blockId} ${block.transaction}")
                val video = block.transaction.get("video")
                val torrent = block.transaction.get("torrent")
                Log.d("Detoks", "Received like for $video, $torrent")
            }
        })
    }
}
