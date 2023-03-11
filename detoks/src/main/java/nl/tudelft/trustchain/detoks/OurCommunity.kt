package nl.tudelft.trustchain.detoks

import android.util.Log
import nl.tudelft.ipv8.Community
import nl.tudelft.ipv8.messaging.Packet

class OurCommunity : Community() {
    override val serviceId = "12313685c1912a191279f8248fc8db5899c5df6a"

    private val MESSAGE_ID = 1

    init {
        messageHandlers[MESSAGE_ID] = ::onMessage
    }

    private fun onMessage(packet: Packet) {
        val (peer, payload) = packet.getAuthPayload(Token.Deserializer)
        Log.d("OurCommunity", peer.mid + ": " + payload.unique_id)
    }

    fun broadcastGreeting() {
        for (peer in getPeers()) {
            val packet = serializePacket(MESSAGE_ID, Token("Hello"))
            send(peer.address, packet)
        }
    }
}

