package nl.tudelft.trustchain.atomicswap

import android.util.Log
import nl.tudelft.ipv8.IPv4Address
import nl.tudelft.ipv8.Community
import nl.tudelft.ipv8.Peer
import nl.tudelft.ipv8.messaging.*
import nl.tudelft.ipv8.messaging.payload.IntroductionResponsePayload
import java.util.*

class AtomicSwapCommunity : Community() {
    override val serviceId = "abcdefabcdefabcdefabcdefabcdef0123456789"
    val discoveredAddressesContacted: MutableMap<IPv4Address, Date> = mutableMapOf()
    val lastTrackerResponses = mutableMapOf<IPv4Address, Date>()

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

    init {
        messageHandlers[Companion.BROADCAST_TRADE_MESSAGE_ID] = ::onTradeOfferMessage
        messageHandlers[Companion.ACCEPT_MESSAGE_ID] = ::onAcceptMessage
        messageHandlers[Companion.INITIATE_MESSAGE_ID] = ::onInitiateMessage
        messageHandlers[Companion.COMPLETE_MESSAGE_ID] = ::onCompleteTrade
    }

    private fun onTradeOfferMessage(packet: Packet) {
        val (peer, payload) = packet.getAuthPayload(TradeMessage.Deserializer)
        Log.d("DemoCommunity", peer.mid + ":PAYLOAD: " + payload.offerId)
        // send back accept
        send(peer.address, serializePacket(Companion.ACCEPT_MESSAGE_ID, AcceptMessage(payload.offerId, peer.mid)))
    }

    private fun onAcceptMessage(packet: Packet) {
        val (peer, payload) = packet.getAuthPayload(AcceptMessage.Deserializer)
        val hash = "abcd"
        val txId = "1234"
        Log.d("DemoCommunity", peer.mid + ": got trade accept ")
        /*
        BTC code goes here
            choose secret
            create swap script
         */
        // send initiate
        Log.d("DemoCommunity", peer.mid + ": SENDING INITIATE")
        send(peer.address, serializePacket(Companion.INITIATE_MESSAGE_ID, InitiateMessage(payload.offerId, hash, txId, peer.mid)))
    }

    private fun onInitiateMessage(packet: Packet) {
        val (peer, payload) = packet.getAuthPayload(InitiateMessage.Deserializer)
        /* Create and broadcast tx script */
        Log.d("DemoCommunity", peer.mid + ": SENDING COMPLETE MESSAGE")
        send(peer.address, serializePacket(Companion.COMPLETE_MESSAGE_ID, CompleteSwapMessage(payload.offerId, peer.mid)))
    }

    private fun onCompleteTrade(packet: Packet) {
        val (peer, payload) = packet.getAuthPayload(CompleteSwapMessage.Deserializer)
        // tell user that trade is complete
        Log.d("DemoCommunity", peer.mid + ": TRADE COMPLETED " + payload.offerId)
    }

    fun broadcastTradeOffer(offerId: Int, amount: Double) {
        for (peer in getPeers()) {
            val packet = serializePacket(
                Companion.BROADCAST_TRADE_MESSAGE_ID, TradeMessage(offerId.toString(),
                    TradeConstants.BITCOIN,
                    TradeConstants.BITCOIN,
                    amount.toString(),
                    amount.toString()))
            send(peer.address, packet)
        }
    }

    companion object {
        private const val BROADCAST_TRADE_MESSAGE_ID = 1
        private const val ACCEPT_MESSAGE_ID = 2
        private const val INITIATE_MESSAGE_ID = 3
        private const val COMPLETE_MESSAGE_ID = 4
    }
}
