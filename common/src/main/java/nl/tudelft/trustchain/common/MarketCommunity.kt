package nl.tudelft.trustchain.common

import android.util.Log
import nl.tudelft.ipv8.Community
import nl.tudelft.ipv8.IPv4Address
import nl.tudelft.ipv8.Peer
import nl.tudelft.ipv8.messaging.Packet
import nl.tudelft.ipv8.messaging.payload.IntroductionResponsePayload
import nl.tudelft.ipv8.util.sha1
import nl.tudelft.ipv8.util.toHex
import nl.tudelft.trustchain.common.messaging.TradePayload
import java.util.*

class MarketCommunity : Community() {
    override val serviceId =
        sha1("4c69624e61434c504b3ab5bb7dc5a3a61de442585122b24c9f752469a212dc6d8ffa3d42bbf9c2f8d10ba569b270f615ef78aeff0547f38745d22af268037ad64935ee7c054b7921b23b".toByteArray()).toHex()

    val discoveredAddressesContacted: MutableMap<IPv4Address, Date> = mutableMapOf()
    private val lastTrackerResponses = mutableMapOf<IPv4Address, Date>()
    private val listenersMap: MutableMap<TradePayload.Type?, MutableList<(TradePayload) -> Unit>> = mutableMapOf()

    init {
        messageHandlers[MessageId.TRADE.index] = ::onTradePacket
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

    fun broadcast(payload: TradePayload) {
        val packet = serializePacket(MessageId.TRADE.index, payload)
        for (peer in getPeers().filter { it != myPeer }) {
            send(peer, packet)
        }
    }

    private fun onTradePacket(packet: Packet) {
        val payload = packet.getAuthPayload(TradePayload.Deserializer).second
        notifyListeners(payload)
        Log.d("MarketCommunity::onTradePacket", "Received packet:{${payload.primaryCurrency}, ${payload.secondaryCurrency}, ${payload.amount}, ${payload.price}, ${payload.type}}")
    }

    fun addListener(type: TradePayload.Type?, listener: (TradePayload) -> Unit) {
        val listeners = listenersMap[type] ?: mutableListOf()
        listeners.add(listener)
        listenersMap[type] = listeners
    }

    fun removeListener(type: TradePayload.Type?, listener: (TradePayload) -> Unit) {
        listenersMap[type]?.remove(listener)
    }

    private fun notifyListeners(payload: TradePayload) {
        val universalListeners = listenersMap[null] ?: listOf<(TradePayload) -> Unit>()
        for (listener in universalListeners) {
            listener(payload)
        }

        val listeners = listenersMap[payload.type] ?: listOf<(TradePayload) -> Unit>()
        for (listener in listeners) {
            listener(payload)
        }
    }

    enum class MessageId(val index: Int) {
        TRADE(1)
    }
}
