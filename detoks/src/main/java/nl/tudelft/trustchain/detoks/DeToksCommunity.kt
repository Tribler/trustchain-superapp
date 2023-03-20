package nl.tudelft.trustchain.detoks

import android.content.Context
import android.util.Log
import nl.tudelft.ipv8.Community
import nl.tudelft.ipv8.Overlay
import nl.tudelft.ipv8.Peer
import nl.tudelft.ipv8.messaging.Packet
import nl.tudelft.ipv8.messaging.Serializable
import nl.tudelft.trustchain.detoks.gossiper.BootGossiper
import nl.tudelft.trustchain.detoks.gossiper.NetworkSizeGossiper
import nl.tudelft.trustchain.detoks.gossiper.GossipMessage


class DeToksCommunity(private val context: Context) : Community() {

    private val walletManager = WalletManager(context)
    private val visitedPeers  = mutableListOf<Peer>()

    init {
        messageHandlers[MESSAGE_TORRENT_ID] = ::onTorrentGossip
        messageHandlers[MESSAGE_TRANSACTION_ID] = ::onTransactionMessage
        messageHandlers[MESSAGE_WATCH_TIME_ID] = :: onWatchTimeGossip
        messageHandlers[MESSAGE_NETWORK_SIZE_ID] = :: onNetworkSizeGossip
        messageHandlers[MESSAGE_BOOT_REQUEST] = :: onBootRequestGossip
        messageHandlers[MESSAGE_BOOT_RESPONSE] = :: onBootResponseGossip

    }

    companion object {
        const val LOGGING_TAG = "DeToksCommunity"
        const val MESSAGE_TORRENT_ID = 1
        const val MESSAGE_TRANSACTION_ID = 2
        const val MESSAGE_WATCH_TIME_ID = 3
        const val MESSAGE_NETWORK_SIZE_ID = 4
        const val MESSAGE_BOOT_REQUEST = 5
        const val MESSAGE_BOOT_RESPONSE = 6
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

    fun gossipWith(peer: Peer, message: Serializable, id: Int) {
        Log.d(LOGGING_TAG, "Gossiping with ${peer.mid}, address: ${peer.address}, msg id: $id")
        Log.d(LOGGING_TAG, this.getPeers().toString())
        Log.d(LOGGING_TAG, this.myPeer.toString())
        Log.d(LOGGING_TAG, "My wallet size: ${walletManager.getOrCreateWallet(myPeer.mid)}")
        Log.d(LOGGING_TAG, "My peer wallet size: ${walletManager.getOrCreateWallet(peer.mid)}")

        val packet = serializePacket(id, message)

        // Send a token only to a new peer
        if (!visitedPeers.contains(peer)) {
            visitedPeers.add(peer)
            sendTokens(1, peer.mid)
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
//        val (peer, payload) = packet.getAuthPayload(TorrentMessage.Deserializer)
        val payload = packet.getPayload(GossipMessage.Deserializer)
        val torrentManager = TorrentManager.getInstance(context)
        //Log.d("DeToksCommunity", "received torrent from ${peer.mid}, address: ${peer.address}, magnet: ${payload.magnet}")
        Log.d(LOGGING_TAG, "magnet: ${payload.data}")
        payload.data.forEach { torrentManager.addTorrent(it as String) }
    }

    private fun onWatchTimeGossip(packet: Packet) {
        val (peer, payload) = packet.getAuthPayload(GossipMessage.Deserializer)
        val torrentManager = TorrentManager.getInstance(context)
        Log.d(LOGGING_TAG, "Received watch time entry from ${peer.mid}, payload: ${payload.data}")

        payload.data.forEach {
            torrentManager.profile.updateEntryWatchTime(
                (it as Pair<*, *>).first as String, it.second as Long,
                false
            )
        }
    }

    private fun onNetworkSizeGossip(packet: Packet) {
        val (peer, payload) = packet.getAuthPayload(GossipMessage.Deserializer)
        NetworkSizeGossiper.receivedResponse(payload, peer)
    }

    private fun onBootRequestGossip(packet: Packet) {
        val (peer, _) = packet.getAuthPayload(GossipMessage.Deserializer)
        BootGossiper.sendResponse(peer)
    }

    private fun onBootResponseGossip(packet: Packet) {
        val (_, payload) = packet.getAuthPayload(GossipMessage.Deserializer)
        BootGossiper.receivedResponse(payload.data)
    }

    class Factory(
        private val context: Context
    ) : Overlay.Factory<DeToksCommunity>(DeToksCommunity::class.java) {
        override fun create(): DeToksCommunity {
            return DeToksCommunity(context)
        }
    }
}

