package nl.tudelft.ipv8.android.demo

import android.util.Log
import nl.tudelft.ipv8.Address
import nl.tudelft.ipv8.Community
import nl.tudelft.ipv8.Peer
import nl.tudelft.ipv8.messaging.Deserializable
import nl.tudelft.ipv8.messaging.Packet
import nl.tudelft.ipv8.messaging.Serializable
import nl.tudelft.ipv8.messaging.payload.GlobalTimeDistributionPayload
import nl.tudelft.ipv8.messaging.payload.IntroductionResponsePayload
import java.util.*

class DemoCommunity : Community() {
    override val serviceId = "02313685c1912a141279f8248fc8db5899c5df5a"

    init {
        messageHandlers[1] = ::onMessage
    }

    val discoveredAddresses: MutableSet<Address> = mutableSetOf()
    val discoveredAddressesIntroduced: MutableMap<Address, Date> = mutableMapOf()
    val discoveredAddressesContacted: MutableMap<Address, Date> = mutableMapOf()

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

    override fun onIntroductionResponse(
        peer: Peer,
        dist: GlobalTimeDistributionPayload,
        payload: IntroductionResponsePayload
    ) {
        super.onIntroductionResponse(peer, dist, payload)

        Log.d("DemoCommunity", "onIntroductionResponse $payload")
    }

    override fun discoverAddress(peer: Peer, address: Address, serviceId: String) {
        super.discoverAddress(peer, address, serviceId)

        discoveredAddresses.add(address)
        discoveredAddressesIntroduced[address] = Date()

        // TODO: remove old addresses
    }

    override fun walkTo(address: Address) {
        super.walkTo(address)

        discoveredAddressesContacted[address] = Date()
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
