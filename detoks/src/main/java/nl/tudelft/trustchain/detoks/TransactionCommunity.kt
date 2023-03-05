package nl.tudelft.trustchain.detoks

import MyMessage
import mu.KotlinLogging
import nl.tudelft.ipv8.Community
import nl.tudelft.ipv8.messaging.Packet

private const val MESSAGE_ID = 1
private val logger = KotlinLogging.logger {}

class TransactionCommunity: Community() {
    override val serviceId = "02313685c1912a144279f8248fc3db5899c5df8c"


    init {
        messageHandlers[MESSAGE_ID] = ::onMessage
    }

    private fun onMessage(packet: Packet) {
        val (peer, payload) = packet.getAuthPayload(MyMessage.Deserializer)
        logger.debug("DemoCommunity", peer.mid + ": " + payload.message)
    }

    fun broadcastGreeting() {
        for (peer in getPeers()) {
            val packet = serializePacket(MESSAGE_ID, MyMessage("Hello!"))
            send(peer.address, packet)
        }
    }
}
