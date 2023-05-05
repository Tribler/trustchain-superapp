package nl.tudelft.trustchain.detoks

import android.content.Context
import android.util.Log
import nl.tudelft.ipv8.Overlay
import nl.tudelft.ipv8.Peer
import nl.tudelft.ipv8.messaging.Packet
import nl.tudelft.ipv8.messaging.Serializable
import nl.tudelft.trustchain.detoks.gossiper.BootGossiper
import nl.tudelft.trustchain.detoks.gossiper.BootMessage
import nl.tudelft.trustchain.detoks.gossiper.NetworkSizeGossiper
import nl.tudelft.trustchain.detoks.gossiper.NetworkSizeMessage
import nl.tudelft.trustchain.detoks.gossiper.TorrentGossiper
import nl.tudelft.trustchain.detoks.gossiper.TorrentMessage


class DeToksCommunity(
    private val context: Context
    ) : TransactionEngine(DetoksConfig.DETOKS_SERVICE_ID) {

    private val walletManager = WalletManager(context)
    private val visitedPeers  = mutableListOf<Peer>()
    private lateinit var libtorrentPort: String


    init {
        messageHandlers[MESSAGE_TORRENT_ID]       = ::onTorrentGossip
        messageHandlers[MESSAGE_TRANSACTION_ID]   = ::onTransactionMessage
        messageHandlers[MESSAGE_NETWORK_SIZE_ID]  = ::onNetworkSizeGossip
        messageHandlers[MESSAGE_BOOT_REQUEST]     = ::onBootRequestGossip
        messageHandlers[MESSAGE_BOOT_RESPONSE]    = ::onBootResponseGossip
        messageHandlers[MESSAGE_TOKEN_REQUEST_ID] = ::onTokenRequestMessage
        messageHandlers[MESSAGE_PORT_REQUEST_ID]  = ::onPortRequestMessage
    }

    companion object {
        const val LOGGING_TAG               = "DeToksCommunity"
        const val MESSAGE_TORRENT_ID        = 1
        const val MESSAGE_TRANSACTION_ID    = 2
        const val MESSAGE_NETWORK_SIZE_ID   = 3
        const val MESSAGE_BOOT_REQUEST      = 4
        const val MESSAGE_BOOT_RESPONSE     = 5
        const val MESSAGE_TOKEN_REQUEST_ID  = 6
        const val MESSAGE_PORT_REQUEST_ID   = 7

    }

    override val serviceId = "c86a7db45eb3563ae047639817baec4db2bc7c25"

    fun sendTokens(amount: Float, recipientMid: String) {
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
            Log.e(LOGGING_TAG, "Insufficient funds!")
        }

    }
    fun increaseTokens(amount: Float) {
        val x = walletManager.getOrCreateWallet(myPeer.mid)
        walletManager.setWalletBalance(myPeer.mid, x.balance + amount)
    }

    fun saveLibTorrentPort(port: String) {
        libtorrentPort = port
    }

    fun gossipWith(peer: Peer, message: Serializable, id: Int) {
        Log.d(LOGGING_TAG, "Gossiping with ${peer.mid}, msg id: $id")

        val packet = serializePacket(id, message)

        // Send a token only to a new peer
        if (!visitedPeers.contains(peer)) {
            visitedPeers.add(peer)
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
        TorrentGossiper.receivedResponse(payload.data, context)
    }

    private fun onPortRequestMessage(packet: Packet) {

        Log.d(LOGGING_TAG, "here?")
        try {
            val (_, payload) = packet.getAuthPayload(PortRequestMessage.Deserializer)

            Log.d(LOGGING_TAG, "Received port request from ${payload.senderMid}")

            // Check if the received port matches the libtorrent port
            if (payload.port == libtorrentPort) {
                // If the port matches, you can call the sendTokens function or perform any other action
                sendTokens(1.0f, payload.senderMid)
            } else {
                Log.d(
                    LOGGING_TAG,
                    "Received port ${payload.port} does not match libtorrent port $libtorrentPort"
                )
            }
        } catch (e: Exception) {
            Log.e(LOGGING_TAG, "Error deserializing PortRequestMessage payload: ${e.message}")
        }
    }


    fun requestPeerLibtorrentPort(recipientMid: String, port: String) {
        if (myPeer.mid == recipientMid) {
            Log.d(LOGGING_TAG, "Cannot request port from yourself.")
            return
        }

        Log.d(LOGGING_TAG, "Requesting libtorrent port from $recipientMid")

        // Find the peer by its mid
        val recipientPeer = getPeers().find { it.mid == recipientMid }

        // If the peer is found, send a port request message
        if (recipientPeer != null) {
            val requestMessage = PortRequestMessage(myPeer.mid, port)
            val packet = serializePacket(
                MESSAGE_PORT_REQUEST_ID,
                requestMessage
            )
            send(recipientPeer.address, packet)
            Log.d(LOGGING_TAG, "sent message")
        } else {
            Log.d(LOGGING_TAG, "Peer not found: $recipientMid")
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
    fun findPeerByAddress(ip: String, port: String) {
        Log.d(LOGGING_TAG, "calling findPeerByAddress with ip $ip and port $port"  )
        Log.d(LOGGING_TAG, " $port mylibtorrent port: $libtorrentPort actual port: ${myPeer.address.port}"  )
       val peerlist =  getPeers().filter {
            print(port)
            it.address.ip == ip }

        for (peer in peerlist) {
            requestPeerLibtorrentPort(peer.mid, port)
        }


    }
    fun findPeerByIps(ip: String): List<Peer?> {
        return getPeers().filter { it.address.ip == ip  }
    }
    fun getBalance(): Float {
        return walletManager.getOrCreateWallet(myPeer.mid).balance
    }
    class Factory(
        private val context: Context
    ) : Overlay.Factory<DeToksCommunity>(DeToksCommunity::class.java) {
        override fun create(): DeToksCommunity {
            return DeToksCommunity(context)
        }
    }
}

