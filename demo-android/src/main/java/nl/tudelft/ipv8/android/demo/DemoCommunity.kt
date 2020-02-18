package nl.tudelft.ipv8.android.demo

import android.util.Log
import nl.tudelft.ipv8.Community
import nl.tudelft.ipv8.attestation.trustchain.payload.CrawlRequestPayload
import nl.tudelft.ipv8.messaging.Deserializable
import nl.tudelft.ipv8.messaging.Packet
import nl.tudelft.ipv8.messaging.Serializable

class DemoCommunity : Community() {
    override val serviceId = "02313685c1912a141279f8248fc8db5899c5df5a"

    init {
        messageHandlers[1] = ::onMessage
    }

    private fun onMessage(packet: Packet) {
        val (peer, payload) = packet.getAuthPayload(MyMessage.Companion, cryptoProvider)
        Log.d("DemoCommunity", peer.mid + ": " + payload.message)
    }

    fun broadcastGreeting() {
        for (peer in getPeers()) {
            val packet = serializePacket(1, listOf(MyMessage("Hello!")))
            send(peer.address, packet)
        }
    }

    class MyMessage(val message: String) : Serializable {
        override fun serialize(): ByteArray {
            return message.toByteArray()
        }

        companion object : Deserializable<MyMessage> {
            override fun deserialize(buffer: ByteArray, offset: Int): Pair<MyMessage, Int> {
                return Pair(MyMessage(buffer.toString(Charsets.UTF_8)), buffer.size)
            }
        }
    }
}
