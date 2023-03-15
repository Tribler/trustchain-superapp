package nl.tudelft.trustchain.detoks

import android.content.Context
import android.util.Log
import nl.tudelft.ipv8.Community
import nl.tudelft.ipv8.IPv4Address
import nl.tudelft.ipv8.Overlay
import nl.tudelft.ipv8.Peer
import nl.tudelft.ipv8.messaging.Packet

class DeToksCommunity(private val context: Context) : Community() {

    init {
        messageHandlers[MESSAGE_TORRENT_ID] = ::onGossip
    }

    companion object {
        const val MESSAGE_TORRENT_ID = 1
    }

    override val serviceId = "c86a7db45eb3563ae047639817baec4db2bc7c25"

    fun gossipWith(peer: Peer) {
        Log.d("DeToksCommunity", "Gossiping with ${peer.mid}, address: ${peer.address}")
        Log.d("DeToksCommunity", this.getPeers().toString())
        Log.d("DeToksCommunity", this.myPeer.toString())
        val listOfTorrents = TorrentManager.getInstance(context).getListOfTorrents()
        if(listOfTorrents.isEmpty()) return
        val magnet = listOfTorrents.random().makeMagnetUri()
        val packet = serializePacket(MESSAGE_TORRENT_ID, TorrentMessage(magnet))

        send(peer.address, packet)
    }

    private fun onGossip(packet: Packet) {
//        val (peer, payload) = packet.getAuthPayload(TorrentMessage.Deserializer)
        val payload = packet.getPayload(TorrentMessage.Deserializer)
        val torrentManager = TorrentManager.getInstance(context)
        //Log.d("DeToksCommunity", "received torrent from ${peer.mid}, address: ${peer.address}, magnet: ${payload.magnet}")
        Log.d("DeToksCommunity", "magnet: ${payload.magnet}")
        torrentManager.addTorrent(payload.magnet)
    }

    class Factory(
        private val context: Context
    ) : Overlay.Factory<DeToksCommunity>(DeToksCommunity::class.java) {
        override fun create(): DeToksCommunity {
            return DeToksCommunity(context)
        }
    }
}
