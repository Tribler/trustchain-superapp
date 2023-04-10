package nl.tudelft.trustchain.detoks

import android.content.Context
import android.util.Log
import com.frostwire.jlibtorrent.Sha1Hash
import nl.tudelft.ipv8.Community
import nl.tudelft.ipv8.Overlay
import nl.tudelft.ipv8.Peer
import nl.tudelft.ipv8.attestation.trustchain.TrustChainCommunity
import nl.tudelft.ipv8.attestation.trustchain.TrustChainCrawler
import nl.tudelft.ipv8.attestation.trustchain.TrustChainSettings
import nl.tudelft.ipv8.attestation.trustchain.store.TrustChainStore
import nl.tudelft.ipv8.messaging.Deserializable
import nl.tudelft.ipv8.messaging.Packet
import nl.tudelft.ipv8.messaging.Serializable
import nl.tudelft.trustchain.detoks.gossiper.*


class DeToksCommunity(private val context: Context,
                      settings: TrustChainSettings,
                      database: TrustChainStore,
                      crawler: TrustChainCrawler = TrustChainCrawler()
) : TrustChainCommunity(settings, database, crawler) {

    private val walletManager = WalletManager(context)
    private val visitedPeers  = mutableListOf<Peer>()

    init {
        messageHandlers[MESSAGE_TORRENT_ID] = ::onTorrentGossip
        messageHandlers[MESSAGE_TRANSACTION_ID] = ::onTransactionMessage
        messageHandlers[MESSAGE_WATCH_TIME_ID] = :: onWatchTimeGossip
        messageHandlers[MESSAGE_NETWORK_SIZE_ID] = :: onNetworkSizeGossip
        messageHandlers[MESSAGE_BOOT_REQUEST] = :: onBootRequestGossip
        messageHandlers[MESSAGE_BOOT_RESPONSE] = :: onBootResponseGossip
        messageHandlers[MESSAGE_TOKEN_REQUEST_ID] = ::onTokenRequestMessage
    }

    companion object {
        const val LOGGING_TAG = "DeToksCommunity"
        const val MESSAGE_TORRENT_ID = 1
        const val MESSAGE_TRANSACTION_ID = 2
        const val MESSAGE_WATCH_TIME_ID = 3
        const val MESSAGE_NETWORK_SIZE_ID = 4
        const val MESSAGE_BOOT_REQUEST = 5
        const val MESSAGE_BOOT_RESPONSE = 6
        const val BLOCK_TYPE = "detoks_transaction"
        const val MESSAGE_TOKEN_REQUEST_ID = 7
    }

    override val serviceId = "c86a7db45eb3563ae047639817baec4db2bc7c25"


    fun sendTokens(amount: Int, recipientMid: String) {
        val senderWallet = walletManager.getOrCreateWallet(myPeer.mid)

        Log.d(LOGGING_TAG, "my wallet ${senderWallet.balance}")

        if (senderWallet.balance >= amount) {
            Log.d(LOGGING_TAG, "Sending $amount money to $recipientMid")
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
            Log.d(LOGGING_TAG, "Insufficient funds!")
        }

    }
    fun requestTokens(amount: Int, recipientMid: String) {
        if (myPeer.mid == recipientMid) {
            Log.d(LOGGING_TAG, "Cannot request tokens from yourself.")
            return
        }

        Log.d(LOGGING_TAG, "Requesting $amount tokens from $recipientMid")

        // Find the peer by its mid
        val recipientPeer = getPeers().find { it.mid == recipientMid }

        // If the peer is found, send a token request message
        if (recipientPeer != null) {
            val requestMessage = TokenRequestMessage(amount, myPeer.mid, recipientMid)
            gossipWith(recipientPeer, requestMessage, MESSAGE_TOKEN_REQUEST_ID)
        } else {
            Log.d(LOGGING_TAG, "Peer not found: $recipientMid")
        }
    }


    fun gossipWith(peer: Peer, message: Serializable, id: Int) {
        Log.d(LOGGING_TAG, "Gossiping with ${peer.mid}, msg id: $id")

        val packet = serializePacket(id, message)

        // Send a token only to a new peer
        if (!visitedPeers.contains(peer)) {
            visitedPeers.add(peer)
            //sendTokens(1, peer.mid)
            val transaction = mapOf("amount" to 1)
            createProposalBlock(BLOCK_TYPE, transaction, peer.publicKey.keyToBin())
            Log.d(LOGGING_TAG, "Created proposal block")
        }

        send(peer.address, packet)
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

            Log.d(LOGGING_TAG, "Received ${payload.amount} tokens from ${payload.senderMID}")
        } else {
            Log.d(LOGGING_TAG, "Insufficient funds from ${payload.senderMID}!")
        }
    }

    private fun onTorrentGossip(packet: Packet) {
        val (_, payload) = packet.getAuthPayload(TorrentMessage.Deserializer)
        val torrentManager = TorrentManager.getInstance(context)
        payload.data.forEach {
            val hash = MagnetLink.hashFromMagnet(it.first)
            torrentManager.addTorrent(Sha1Hash(hash), it.first)
            torrentManager.profile.updateEntryHopCount(hash, it.second)
        }
    }

    private fun onWatchTimeGossip(packet: Packet) {
        val (_, payload) = packet.getAuthPayload(WatchTimeMessage.Deserializer)
        val torrentManager = TorrentManager.getInstance(context)

        payload.data.forEach {
            torrentManager.profile.updateEntryWatchTime(
                it.first,
                it.second,
                false
            )
        }
    }

    private fun onNetworkSizeGossip(packet: Packet) {
        val (peer, payload) = packet.getAuthPayload(NetworkSizeMessage.Deserializer)
        NetworkSizeGossiper.receivedResponse(payload, peer)
    }

    private fun onBootRequestGossip(packet: Packet) {
        val (peer, _) = packet.getAuthPayload(BootMessage.Deserializer)
        BootGossiper.receivedRequest(peer)
    }

    private fun onBootResponseGossip(packet: Packet) {
        val (_, payload) = packet.getAuthPayload(BootMessage.Deserializer)
        BootGossiper.receivedResponse(payload.data)
    }

    private fun onTokenRequestMessage(packet: Packet) {
        val (_, payload) = packet.getAuthPayload(TokenRequestMessage.Deserializer)

        Log.d(LOGGING_TAG, "Received token request from ${payload.senderMid}")


         sendTokens(payload.amount, payload.senderMid)
    }
    fun findPeerByAddress(address: String): Peer? {
        return getPeers().firstOrNull {

            it.address.ip == address }
    }
    fun getBalance(): Float {
        return walletManager.getOrCreateWallet(myPeer.mid).balance
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

