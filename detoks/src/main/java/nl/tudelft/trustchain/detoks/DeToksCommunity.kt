package nl.tudelft.trustchain.detoks

import android.util.Log
import nl.tudelft.ipv8.Community
import nl.tudelft.ipv8.IPv4Address
import nl.tudelft.ipv8.messaging.Packet

class DeToksCommunity() : Community() {

    init {
        messageHandlers[MESSAGE_TORRENT_ID] = ::onMessage
    }

    companion object {
        const val MESSAGE_TORRENT_ID = 1
    }

    override val serviceId = "c86a7db45eb3563ae047639817baec4db2bc7c25"

    override fun walkTo(address: IPv4Address) {
        super.walkTo(address)

        Log.d("DeToksCommunity", this.getPeers().toString())
        broadcastTorrent() // FOR TESTING PURPOSES
    }

    fun broadcastTorrent() {
        for (peer in getPeers()) {
            Log.d("DeToksCommunity", "Sending torrent to $peer")
            val packet = serializePacket(MESSAGE_TORRENT_ID, TorrentMessage("This is a torrent!"))
            send(peer.address, packet)
        }
    }

    private fun onMessage(packet: Packet) {
        val (peer, payload) = packet.getAuthPayload(TorrentMessage.Deserializer)
        Log.d("DeToksCommunity", peer.mid + ": " + payload.message)
    }
}
