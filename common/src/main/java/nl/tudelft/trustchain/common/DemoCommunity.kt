package nl.tudelft.trustchain.common

import android.util.Log
import nl.tudelft.ipv8.IPv4Address
import nl.tudelft.ipv8.Community
import nl.tudelft.ipv8.Peer
import nl.tudelft.ipv8.android.IPv8Android
import nl.tudelft.ipv8.attestation.trustchain.TrustChainCommunity
import nl.tudelft.ipv8.messaging.*
import nl.tudelft.ipv8.messaging.payload.IntroductionResponsePayload
import java.util.*

class DemoCommunity : Community() {
    override val serviceId = "02313685c1912a141279f8248fc8db5899c5df5a"

    val discoveredAddressesContacted: MutableMap<IPv4Address, Date> = mutableMapOf()

    val lastTrackerResponses = mutableMapOf<IPv4Address, Date>()

    // Retrieve the trustchain community
    private fun getTrustChainCommunity(): TrustChainCommunity {
        return IPv8Android.getInstance().getOverlay()
            ?: throw IllegalStateException("TrustChainCommunity is not configured")
    }

    // Create a votingHelper instance for voting across the community
    // val votingHelper: VotingHelper = VotingHelper(getTrustChainCommunity())

    override fun walkTo(address: IPv4Address) {
        super.walkTo(address)

        discoveredAddressesContacted[address] = Date()
    }

    override fun onIntroductionResponse(peer: Peer, payload: IntroductionResponsePayload) {
        super.onIntroductionResponse(peer, payload)

        if (peer.address in DEFAULT_ADDRESSES) {
            lastTrackerResponses[peer.address] = Date()
        }
    }

    object MessageId {
        const val PUNCTURE_REQUEST = 250
        const val PUNCTURE = 249
        const val INTRODUCTION_REQUEST = 246
        const val INTRODUCTION_RESPONSE = 245
        const val THALIS_MESSAGE = 222
    }

    // SEND MESSAGE
    fun broadcastGreeting() {
        for (peer in getPeers()) {
            val packet = serializePacket(MessageId.THALIS_MESSAGE, MyMessage("Hello from Freedom of Computing!"), true)
            send(peer.address, packet)
        }
    }

    // RECEIVE MESSAGE
    init {
        messageHandlers[MessageId.THALIS_MESSAGE] = ::onMessage
    }

    private fun onMessage(packet: Packet) {
        val (peer, payload) = packet.getAuthPayload(MyMessage.Deserializer)
        Log.i("personal", peer.mid + ": " + payload.message)
    }
}

// THE MESSAGE (CLASS)
data class MyMessage(val message: String) : nl.tudelft.ipv8.messaging.Serializable {
    override fun serialize(): ByteArray {
        return message.toByteArray()
    }

    companion object Deserializer : Deserializable<MyMessage> {
        override fun deserialize(buffer: ByteArray, offset: Int): Pair<MyMessage, Int> {
            var toReturn = buffer.toString(Charsets.UTF_8)
            return Pair(MyMessage(toReturn), buffer.size)
        }
    }
}
