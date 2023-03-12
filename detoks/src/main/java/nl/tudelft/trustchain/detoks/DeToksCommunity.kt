package nl.tudelft.trustchain.detoks

import android.content.Context
import android.util.Log
import nl.tudelft.ipv8.Community
import nl.tudelft.ipv8.Overlay
import nl.tudelft.ipv8.Peer
import nl.tudelft.ipv8.IPv4Address
import nl.tudelft.ipv8.messaging.Packet

data class Wallet(var balance: Int = 10) {
}

class DeToksCommunity(private val context: Context) : Community() {
    private val wallets = mutableMapOf<String, Wallet>()

    init {
        messageHandlers[MESSAGE_TORRENT_ID] = ::onGossip
        messageHandlers[MESSAGE_TRANSACTION_ID] = ::onTransactionMessage

    }

    companion object {
        const val MESSAGE_TORRENT_ID = 1
        const val MESSAGE_TRANSACTION_ID = 2

    }

    override val serviceId = "c86a7db45eb3563ae047639817baec4db2bc7c25"

    private fun getOrCreateWallet(peerMid: String): Wallet {
        return wallets.getOrPut(peerMid) { Wallet() }
    }

    fun sendTokens(amount: Int, recipientMid: String) {
        val senderWallet = getOrCreateWallet(myPeer.mid)

        if (senderWallet.balance >= amount) {
            senderWallet.balance -= amount
            val recipientWallet = getOrCreateWallet(recipientMid)
            recipientWallet.balance += amount

            for (peer in getPeers()) {
                val packet = serializePacket(
                    MESSAGE_TRANSACTION_ID,
                    TransactionMessage(amount, myPeer.mid, recipientMid)
                )
                send(peer.address, packet)
            }
        } else {
            Log.d("DeToksCommunity", "Insufficient funds!")
        }
    }

    fun gossipWith(peer: Peer) {
        Log.d("DeToksCommunity", "Gossiping with ${peer.mid}, address: ${peer.address}")

        val listOfTorrents = TorrentManager.getInstance(context).getListOfTorrents()
        if(listOfTorrents.isEmpty()) return
        val magnet = listOfTorrents.random().makeMagnetUri()
        val packet = serializePacket(MESSAGE_TORRENT_ID, TorrentMessage(magnet))

        send(peer.address, packet)
    }

    override fun walkTo(address: IPv4Address) {
        super.walkTo(address)
        for  (peer in getPeers()) {
            sendTokens(1, peer.mid)
        }
        println("map:")
        println(wallets)
        Log.d("DeToksCommunity", this.getPeers().toString())
        broadcastTorrent() // FOR TESTING PURPOSES
    }

    fun broadcastTorrent() {
        for (peer in getPeers()) {
            Log.d("DeToksCommunity", "Sending torrent to $peer")
            val packet = serializePacket(MESSAGE_TORRENT_ID, TorrentMessage("This is a torrent!"))
            send(peer.address, packet)
            Log.d("DeToksCommunityWallet", "$wallets ${wallets.size}")
        }
    }
    private fun onGossip(packet: Packet) {
        val (peer, payload) = packet.getAuthPayload(TorrentMessage.Deserializer)
        val torrentManager = TorrentManager.getInstance(context)
        Log.d("DeToksCommunity", "received torrent from ${peer.mid}, address: ${peer.address}, magnet: ${payload.magnet}")

        torrentManager.addTorrent(payload.magnet)
    }
    private fun onTransactionMessage(packet: Packet) {
        val (_, payload) = packet.getAuthPayload(TransactionMessage.Deserializer)
        println("deserializer: ")
        println(payload)
        val senderWallet = getOrCreateWallet(payload.senderMID)
        if (senderWallet.balance >= payload.amount) {
            senderWallet.balance -= payload.amount
            val recipientWallet = getOrCreateWallet(payload.recipientMID)
            recipientWallet.balance += payload.amount

            Log.d("DeToksCommunity", "Received ${payload.amount} tokens from ${payload.senderMID}")
        } else {
            Log.d("DeToksCommunity", "Insufficient funds from ${payload.senderMID}!")
        }
    }

    class Factory(
        private val context: Context
    ) : Overlay.Factory<DeToksCommunity>(DeToksCommunity::class.java) {
        override fun create(): DeToksCommunity {
            return DeToksCommunity(context)
        }
    }
}
