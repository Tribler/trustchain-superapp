package nl.tudelft.trustchain.atomicswap

import android.util.Log
import nl.tudelft.ipv8.Community
import nl.tudelft.ipv8.IPv4Address
import nl.tudelft.ipv8.Peer
import nl.tudelft.ipv8.messaging.Packet
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

    /**
     *
    typealias onAccept = (AcceptMessage) -> Unit
    typealias onInitiate = (InitiateMessage) -> Unit
    typealias onTrade = (TradeMessage) -> Unit
    typealias onComplete = (CompleteSwapMessage) ->Unit
     */

    private lateinit var onAcceptCallback: onAccept
    private lateinit var onInitiateCallback: onInitiate
    private lateinit var onTradeCallback: onTrade
    private lateinit var onCompleteCallback: onComplete

    fun setOnAccept(callback: onAccept) = callback.also { onAcceptCallback = it }
    fun setOnInitiate(callback: onInitiate) = callback.also { onInitiateCallback = it }
    fun setOnTrade(callback: onTrade) = callback.also { onTradeCallback = it }
    fun setOnComplete(callback: onComplete) = callback.also { onCompleteCallback = it }

    private fun onTradeOfferMessage(packet: Packet) {
        val (peer, payload) = packet.getAuthPayload(TradeMessage.Deserializer)
        Log.d("AtomicSwapCommunity", peer.mid + ":PAYLOAD: " + payload.offerId)
        // send back accept

        onTradeCallback(payload)

        send(
            peer.address,
            serializePacket(Companion.ACCEPT_MESSAGE_ID, AcceptMessage(payload.offerId, peer.mid))
        )
    }

    private fun onAcceptMessage(packet: Packet) {
        val (peer, payload) = packet.getAuthPayload(AcceptMessage.Deserializer)
//        val hash = "abcd"
//        val txId = "1234"
        Log.d("AtomicSwapCommunity", peer.mid + ": got trade accept ")
        /*
        BTC code goes here
            choose secret
            create swap script
         */

        val (hash, txId, publicKey) = onAcceptCallback(payload)


        // send initiate
        Log.d("AtomicSwapCommunity", peer.mid + ": SENDING INITIATE")
        send(
            peer.address,
            serializePacket(
                Companion.INITIATE_MESSAGE_ID,
                InitiateMessage(payload.offerId, hash, txId, publicKey)
            )
        )
    }

    private fun onInitiateMessage(packet: Packet) {
        val (peer, payload) = packet.getAuthPayload(InitiateMessage.Deserializer)
        /* Create and broadcast tx script */

        val txId = onInitiateCallback(payload)

        Log.d("AtomicSwapCommunity", peer.mid + ": SENDING COMPLETE MESSAGE")
        send(
            peer.address,
            serializePacket(
                Companion.COMPLETE_MESSAGE_ID,
                CompleteSwapMessage(payload.offerId, txId)
            )
        )
    }

    private fun onCompleteTrade(packet: Packet) {
        val (peer, payload) = packet.getAuthPayload(CompleteSwapMessage.Deserializer)
        // tell user that trade is complete

//        onCompleteCallback(payload)

        Log.d("AtomicSwapCommunity", peer.mid + ": TRADE COMPLETED " + payload.offerId)
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

data class OnAcceptReturn(
    val hash: String,
    val txId: String,
    val publicKey: String
)//todo better name
typealias onAccept = (AcceptMessage) -> (OnAcceptReturn)
typealias onInitiate = (InitiateMessage) -> (String)
typealias onTrade = (TradeMessage) -> Unit
typealias onComplete = (CompleteSwapMessage) -> Unit
