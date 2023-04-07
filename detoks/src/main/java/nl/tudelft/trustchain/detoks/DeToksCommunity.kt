package nl.tudelft.trustchain.detoks

import android.content.Context
import android.util.Log
import nl.tudelft.ipv8.Community
import nl.tudelft.ipv8.Overlay
import nl.tudelft.ipv8.Peer
import nl.tudelft.ipv8.messaging.Packet


class DeToksCommunity(private val context: Context) : Community() {

    private val walletManager = WalletManager(context)
    private val visitedPeers  = mutableListOf<Peer>()

    init {
        messageHandlers[MESSAGE_TORRENT_ID] = ::onGossip
        messageHandlers[MESSAGE_TRANSACTION_ID] = ::onTransactionMessage

    }

    companion object {
        const val MESSAGE_TORRENT_ID = 1
        const val MESSAGE_TRANSACTION_ID = 2

    }

    override val serviceId = "c86a7db45eb3563ae047639817baec4db2bc7c25"


    fun sendTokens(amount: Int, recipientMid: String) {
        val senderWallet = walletManager.getOrCreateWallet(myPeer.mid)

        Log.d("DetoksCommunity", "my wallet ${senderWallet.balance}")

        if (senderWallet.balance >= amount) {
            Log.d("DetoksCommunity", "Sending $amount money to $recipientMid")
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
            Log.d("DeToksCommunity", "Insufficient funds!")
        }

    }

    fun gossipWith(peer: Peer) {
        Log.d("DeToksCommunity", "Gossiping with ${peer.mid}, address: ${peer.address}")
        Log.d("DetoksCommunity", "My wallet size: ${walletManager.getOrCreateWallet(myPeer.mid)}")
        Log.d("DetoksCommunity", "My peer wallet size: ${walletManager.getOrCreateWallet(peer.mid)}")
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
        val (peer, payload) = packet.getAuthPayload(TorrentMessage.Deserializer)
        val torrentManager = TorrentManager.getInstance(context)
        Log.d("DeToksCommunity", "received torrent from ${peer.mid}, address: ${peer.address}, magnet: ${payload.magnet}")
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
