
package nl.tudelft.trustchain.common

import android.util.Log
import nl.tudelft.ipv8.Community
import nl.tudelft.ipv8.IPv4Address
import nl.tudelft.ipv8.Peer
import nl.tudelft.ipv8.messaging.Packet
import nl.tudelft.ipv8.messaging.Serializable
import nl.tudelft.ipv8.messaging.payload.IntroductionResponsePayload
import nl.tudelft.trustchain.common.messaging.TradePayload
import java.util.*

class MarketCommunity : Community() {
//    This is the serviceId of the 'real' market community, for development purposes a smaller community is used
//    override val serviceId = "98c1f6342f30528ada9647197f0503d48db9c2fb"
    override val serviceId = "98c1f6342f30528ada9647197f0503d48db9c2fc"

    val discoveredAddressesContacted: MutableMap<IPv4Address, Date> = mutableMapOf()
    val listenersMap: MutableMap<TradePayload.Type?, MutableList<(TradePayload) -> Unit>> = mutableMapOf()
    private val lastTrackerResponses = mutableMapOf<IPv4Address, Date>()

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

    fun broadcast(payload: Serializable) {
        val packet = serializePacket(MessageId.TRADE.index, payload)
        for (peer in getPeers().filter { it != myPeer }) {
            send(peer, packet)
        }
    }

    private fun onTradePacket(packet: Packet) {
        val payload = packet.getAuthPayload(TradePayload.Deserializer).second
        notifyListeners(payload)
        Log.d("MarketCommunity", "Received packet:{${payload.primaryCurrency}, ${payload.secondaryCurrency}, ${payload.amount}, ${payload.price}, ${payload.type}}")
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
