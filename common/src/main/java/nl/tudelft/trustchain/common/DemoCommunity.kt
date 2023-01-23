package nl.tudelft.trustchain.common

import kotlinx.coroutines.ObsoleteCoroutinesApi
import kotlinx.coroutines.channels.BroadcastChannel
import nl.tudelft.ipv8.Community
import nl.tudelft.ipv8.IPv4Address
import nl.tudelft.ipv8.Peer
import nl.tudelft.ipv8.android.IPv8Android
import nl.tudelft.ipv8.attestation.trustchain.TrustChainCommunity
import nl.tudelft.ipv8.messaging.Address
import nl.tudelft.ipv8.messaging.Packet
import nl.tudelft.ipv8.messaging.payload.IntroductionResponsePayload
import nl.tudelft.ipv8.messaging.payload.PuncturePayload
import java.util.*

class DemoCommunity : Community() {
    override val serviceId = "02313685c1912a141279f8248fc8db5899c5df5a"

    val discoveredAddressesContacted: MutableMap<IPv4Address, Date> = mutableMapOf()

    val lastTrackerResponses = mutableMapOf<IPv4Address, Date>()

    @OptIn(ObsoleteCoroutinesApi::class)
    val punctureChannel = BroadcastChannel<Pair<Address, PuncturePayload>>(10000)

    // Retrieve the trustchain community
    private fun getTrustChainCommunity(): TrustChainCommunity {
        return IPv8Android.getInstance().getOverlay()
            ?: throw IllegalStateException("TrustChainCommunity is not configured")
    }

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
        const val PUNCTURE_TEST = 251
    }

    fun sendPuncture(address: IPv4Address, id: Int) {
        val payload = PuncturePayload(myEstimatedLan, myEstimatedWan, id)
        val packet = serializePacket(MessageId.PUNCTURE_TEST, payload, sign = false)
        endpoint.send(address, packet)
    }

    // RECEIVE MESSAGE
    init {
        messageHandlers[MessageId.PUNCTURE_TEST] = ::onPunctureTest
    }

    @OptIn(ObsoleteCoroutinesApi::class)
    private fun onPunctureTest(packet: Packet) {
        val payload = packet.getPayload(PuncturePayload.Deserializer)
        punctureChannel.trySend(Pair(packet.source, payload)).isSuccess
    }
}
