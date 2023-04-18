package nl.tudelft.trustchain.detoks

import android.content.Context
import android.util.Log
import com.frostwire.jlibtorrent.Sha1Hash
import nl.tudelft.ipv8.Community
import nl.tudelft.ipv8.Overlay
import nl.tudelft.ipv8.Peer
import nl.tudelft.ipv8.messaging.Packet
import nl.tudelft.ipv8.messaging.Serializable
import nl.tudelft.trustchain.detoks.gossiper.*
import kotlin.math.max


class DeToksCommunity(private val context: Context) : Community() {

    private val walletManager = WalletManager(context)
    private val visitedPeers  = mutableListOf<Peer>()

    init {
        messageHandlers[MESSAGE_TORRENT_ID] = ::onTorrentGossip
        messageHandlers[MESSAGE_TRANSACTION_ID] = ::onTransactionMessage
        messageHandlers[MESSAGE_PROFILE_ENTRY_ID] = :: onProfileEntryGossip
        messageHandlers[MESSAGE_NETWORK_SIZE_ID] = :: onNetworkSizeGossip
        messageHandlers[MESSAGE_BOOT_REQUEST] = :: onBootRequestGossip
        messageHandlers[MESSAGE_BOOT_RESPONSE] = :: onBootResponseGossip
    }

    companion object {
        const val LOGGING_TAG = "DeToksCommunity"
        const val MESSAGE_TORRENT_ID = 1
        const val MESSAGE_TRANSACTION_ID = 2
        const val MESSAGE_PROFILE_ENTRY_ID = 3
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
        Log.d(LOGGING_TAG, "Gossiping with ${peer.mid}, msg id: $id")

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
        val (_, payload) = packet.getAuthPayload(TorrentMessage.Deserializer)
        val torrentManager = TorrentManager.getInstance(context)
        payload.data.forEach {
            val hash = MagnetLink.hashFromMagnet(it.first)
            torrentManager.addTorrent(Sha1Hash(hash), it.first)
            torrentManager.profile.updateEntryHopCount(hash, it.second)
        }
    }

    private fun onProfileEntryGossip(packet: Packet) {
        val (_, payload) = packet.getAuthPayload(ProfileEntryMessage.Deserializer)
        val data = payload.data
        if(data[0].first != "Key") {
            Log.d(LOGGING_TAG, "Received data in profile entry message that wasn't recognized")
            return
        }
        val key = data[0].second
        val profile = TorrentManager.getInstance(context).profile
        data.drop(0).forEach {
            when(it.first) {
                "WatchTime" -> profile.updateEntryWatchTime(key, it.second.toLong(), false)
                "Likes" -> profile.updateEntryLikes(key, it.second.toInt(), false)
                "Duration" -> profile.profiles[key]!!.duration = max(profile.profiles[key]!!.duration, it.second.toLong())
                "UploadDate" -> profile.profiles[key]!!.uploadDate = max(profile.profiles[key]!!.uploadDate, it.second.toLong())
                else -> Log.d(LOGGING_TAG, "Received data in profile entry message that wasn't recognized")
            }
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

    class Factory(
        private val context: Context
    ) : Overlay.Factory<DeToksCommunity>(DeToksCommunity::class.java) {
        override fun create(): DeToksCommunity {
            return DeToksCommunity(context)
        }
    }
}

