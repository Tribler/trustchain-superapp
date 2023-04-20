package nl.tudelft.trustchain.detoks

import android.content.Context
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import nl.tudelft.ipv8.IPv4Address
import nl.tudelft.ipv8.Overlay
import nl.tudelft.ipv8.Peer
import nl.tudelft.ipv8.android.IPv8Android
import nl.tudelft.ipv8.attestation.trustchain.*
import nl.tudelft.ipv8.attestation.trustchain.store.TrustChainStore
import nl.tudelft.ipv8.messaging.Packet
import nl.tudelft.ipv8.messaging.payload.IntroductionResponsePayload
import java.util.*

const val LIKE_BLOCK: String = "like_block"

class DeToksCommunity(
    private val context: Context,
    settings: TrustChainSettings,
    database: TrustChainStore,
    crawler: TrustChainCrawler = TrustChainCrawler()
) : TrustChainCommunity(settings, database, crawler) {

    override val serviceId = "d86a7db45eb3563ae047639817baec4db2bc7c21"
    private val walletManager = WalletManager(context)
    private val visitedPeers  = mutableListOf<Peer>()
    private val lastTrackerResponses = mutableMapOf<IPv4Address, Date>()

    init {
        registerBlockSigner(LIKE_BLOCK, object : BlockSigner {
            override fun onSignatureRequest(block: TrustChainBlock) {
                Log.wtf("detoks","liker is "+block.transaction["liker"] as String )
                Log.wtf("detoks","video is "+block.transaction["video"] as String)
                createAgreementBlock(block, block.transaction)
            }
        })
        addListener(LIKE_BLOCK, object : BlockListener {
            @RequiresApi(Build.VERSION_CODES.O)
            override fun onBlockReceived(block: TrustChainBlock) {
                Log.d("Detoks", "onBlockReceived: ${block.blockId} ${block.transaction}")
                val video = block.transaction["video"] as String
                val torrent = block.transaction.get("torrent") as String
                val magnet = block.transaction.get("torrentMagnet") as String
                val community = IPv8Android.getInstance().getOverlay<DeToksCommunity>()!!
                val newLikes = community.getLikes(video, torrent).size
                Log.d("Detoks", "Received like for $video, $torrent")
                Log.d("Detoks", "new number of likes: $newLikes")
                // 1 block is always in the DB since we just received it
                if (duplicates(video, torrent) > 1) {
                    Log.wtf("Detoks", "Ignoring magnet for duplicate video: $video, $torrent")
                    return
                }
                TorrentManager.getInstance(context).addMagnet(magnet)
            }
        })
    }

    companion object {
        const val MESSAGE_TORRENT_ID = 1
        const val MESSAGE_TRANSACTION_ID = 2

    }
    fun sendTokens(amount: Int, recipientMid: String) {
        val senderWallet = walletManager.getOrCreateWallet(myPeer.mid)

//        Log.d("DetoksCommunity", "my wallet ${senderWallet.balance}")

        if (senderWallet.balance >= amount) {
//            Log.d("DetoksCommunity", "Sending $amount money to $recipientMid")
            senderWallet.balance -= amount
            walletManager.setWalletBalance(myPeer.mid, senderWallet.balance)

            val recipientWallet = walletManager.getOrCreateWallet(recipientMid)
            recipientWallet.balance += amount
            walletManager.setWalletBalance(recipientMid, recipientWallet.balance)

            for (peer in getPeers()) {
                val packet = serializePacket(
                    MESSAGE_TRANSACTION_ID,
                    TransactionMessage(amount, myPeer.mid, recipientMid)
                )
                send(peer.address, packet)
            }
        } else {
//            Log.d("DeToksCommunity", "Insufficient funds!")
        }

    }

    fun gossipWith(peer: Peer) {
//        Log.d("DeToksCommunity", "Gossiping with ${peer.mid}, address: ${peer.address}")
//        Log.d("DetoksCommunity", "My wallet size: ${walletManager.getOrCreateWallet(myPeer.mid)}")
//        Log.d("DetoksCommunity", "My peer wallet size: ${walletManager.getOrCreateWallet(peer.mid)}")
        val listOfTorrents = TorrentManager.getInstance(context).getListOfTorrents()
        if(listOfTorrents.isEmpty()) return
        val magnet = listOfTorrents.random().makeMagnetUri()

        val packet = serializePacket(MESSAGE_TORRENT_ID, TorrentMessage(magnet))

        // Send a token only to a new peer
        if (!visitedPeers.contains(peer)) {
            visitedPeers.add(peer)
            sendTokens(1, peer.mid)
        }

        send(peer.address, packet)
    }

    private fun onGossip(packet: Packet) {
        val (_, payload) = packet.getAuthPayload(TorrentMessage.Deserializer)
        val torrentManager = TorrentManager.getInstance(context)
//        Log.d("DeToksCommunity", "received torrent from ${peer.mid}, address: ${peer.address}, magnet: ${payload.magnet}")

        torrentManager.addTorrent(payload.magnet)

    }
    private fun onTransactionMessage(packet: Packet) {
        val (_, payload) = packet.getAuthPayload(TransactionMessage.Deserializer)

        val senderWallet = walletManager.getOrCreateWallet(payload.senderMID)

        if (senderWallet.balance >= payload.amount) {
            senderWallet.balance -= payload.amount
            walletManager.setWalletBalance(payload.senderMID, senderWallet.balance)

            val recipientWallet = walletManager.getOrCreateWallet(payload.recipientMID)
            recipientWallet.balance += payload.amount
            walletManager.setWalletBalance(payload.recipientMID, recipientWallet.balance)

//            Log.d("DeToksCommunity", "Received ${payload.amount} tokens from ${payload.senderMID}")
        } else {
//            Log.d("DeToksCommunity", "Insufficient funds from ${payload.senderMID}!")
        }
    }

    override fun onIntroductionResponse(peer: Peer, payload: IntroductionResponsePayload) {
        super.onIntroductionResponse(peer, payload)

        if (peer.address in DEFAULT_ADDRESSES) {
            lastTrackerResponses[peer.address] = Date()
        }
        // We meet a new peer -> crawl their chain
        runBlocking {
            // Blocking!!!
            launch { // launch a new coroutine and continue
                super.crawlChain(peer, null) // Crawl their chain (unknown last block)
            }
        }
    }

    fun broadcastLike(vid: String, torrent: String, creator: String, magnet: String, flag: Boolean = false) {
        if (userLikedVideo(vid, torrent, myPeer.publicKey.toString())) return
        val timestamp = System.currentTimeMillis().toString()
        val like = Like(if (flag) creator else myPeer.publicKey.toString(), vid, torrent, creator, timestamp, magnet)
        createProposalBlock(LIKE_BLOCK, like.toMap(), myPeer.publicKey.keyToBin())
    }

    fun getLikes(vid: String, torrent: String): List<TrustChainBlock> {
        return database.getBlocksWithType(LIKE_BLOCK).filter {
            it.transaction["video"] == vid && it.transaction["torrent"] == torrent
        }.distinctBy { it.transaction["liker"] }
    }
    fun getAllUniqueVideos(): List<Pair<String,String>> {
        val videos = database.getBlocksWithType(LIKE_BLOCK)
        val unique = videos.filter{ video -> videos.count { it.transaction["video"] == video.transaction["video"] && it.transaction["torrent"] == video.transaction["torrent"] } == 1}
        return unique.map { Pair(it.transaction["video"] as String,it.transaction["torrent"] as String) }
    }
    fun getAuthorOfMagnet(magnet: String): String {
        val a = database.getBlocksWithType(LIKE_BLOCK).filter {
            it.transaction["torrentMagnet"] == magnet
        }
        if(a.isEmpty()) return ""
        return a[0].transaction["author"] as String

    }
    fun getBlocksByAuthor(author: String): List<TrustChainBlock> {
        val authorsBlocks =  database.getBlocksWithType(LIKE_BLOCK).filter {
            it.transaction["author"] == author
        }
        if(authorsBlocks.isEmpty()) return authorsBlocks
        return authorsBlocks.sortedWith(compareByDescending{(it.transaction["timestamp"] as String).toLong()})
    }

    fun getNotifications(author: String): List<TrustChainBlock> {
        return getBlocksByAuthor(author).filter { it.transaction["liker"] != author }
    }

    fun getEarliestDate(vid: String, torrent: String): Long {
        if(duplicates(vid,torrent) == 0) return Long.MAX_VALUE
        return (getLikes(vid,torrent).sortedBy{(it.transaction["timestamp"] as String).toLong()}[0].transaction["timestamp"] as String).toLong()
    }

    fun userLikedVideo(vid: String, torrent: String, liker: String): Boolean {
        return getLikes(vid, torrent).any { it.transaction["liker"] == liker }
    }
    fun duplicates(vid: String, torrent: String): Int {
        return database.getBlocksWithType(LIKE_BLOCK).filter { it.transaction["video"] == vid && it.transaction["torrent"] == torrent }.size
    }

    fun getPostedVideos(author: String): List<Pair<String, Int>> {
        // Create Key data class so we can group by two fields (torrent and video)
        data class Key(val video: String, val torrent: String)
        fun TrustChainBlock.toKey() = Key(transaction["video"] as String, transaction["torrent"] as String)
            val likes = getBlocksByAuthor(author).groupBy { it.toKey() }
        return likes.entries.map {
            Pair(it.key.video, it.value.size)
        }
    }

    fun listOfLikedVideosAndTorrents(person: String): List<Pair<String,String>> {
        val iterato = database.getBlocksWithType(LIKE_BLOCK).filter {
            it.transaction["liker"] == person
        }
        if(iterato.isEmpty()) return emptyList()
        val iterator= iterato.sortedWith(compareByDescending{(it.transaction["timestamp"] as String).toLong()}).listIterator()
        val likedVideos = ArrayList<Pair<String,String>>()
        while(iterator.hasNext()) {
            val block = iterator.next()
            likedVideos.add(Pair(block.transaction["video"] as String, block.transaction["torrentMagnet"] as String))
        }
        return likedVideos
    }

    class Factory(
        private val context: Context,
        private val settings: TrustChainSettings,
        private val database: TrustChainStore,
        private val crawler: TrustChainCrawler = TrustChainCrawler()
    ) : Overlay.Factory<DeToksCommunity>(DeToksCommunity::class.java) {
        override fun create(): DeToksCommunity {
            return DeToksCommunity(context, settings, database, crawler)
        }
    }
}
