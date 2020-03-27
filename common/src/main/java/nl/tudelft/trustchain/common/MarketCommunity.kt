package nl.tudelft.trustchain.common

import android.util.Log
import nl.tudelft.ipv8.Community
import nl.tudelft.ipv8.IPv4Address
import nl.tudelft.ipv8.Peer
import nl.tudelft.ipv8.messaging.Packet
import nl.tudelft.ipv8.messaging.payload.IntroductionResponsePayload
import nl.tudelft.ipv8.util.sha1
import nl.tudelft.ipv8.util.toHex
import nl.tudelft.trustchain.common.constants.Currency
import nl.tudelft.trustchain.common.messaging.AskPayload
import java.util.*

class MarketCommunity : Community() {
    override val serviceId =
        sha1("4c69624e61434c504b3ab5bb7dc5a3a61de442585122b24c9f752469a212dc6d8ffa3d42bbf9c2f8d10ba569b270f615ef78aeff0547f38745d22af268037ad64935ee7c054b7921b23b".toByteArray()).toHex()

    val discoveredAddressesContacted: MutableMap<IPv4Address, Date> = mutableMapOf()
    val lastTrackerResponses = mutableMapOf<IPv4Address, Date>()

    init {
        messageHandlers[MessageId.ASK] = ::onAskPacket
        messageHandlers[MessageId.BID] = ::onBidPacket
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

    fun broadcastAsk(payload: AskPayload) {
        val packet = serializePacket(MessageId.ASK, payload)
        for (peer in getPeers()) {
            send(peer, packet)
        }
    }

    private fun onAskPacket(packet: Packet) {
        val payload = packet.getAuthPayload(AskPayload.Deserializer).second
        Log.d("MarketCommunity::onAskPacket", "Received packet:{${payload.askCurrency}, ${payload.paymentCurrency}, ${payload.amount}, ${payload.price}}")
    }

    private fun onBidPacket(packet: Packet) {
        return
    }

    object MessageId {
        const val BID = 1
        const val ASK = 2
    }
}
