package nl.tudelft.trustchain.atomicswap

import android.util.Log
import nl.tudelft.ipv8.Community
import nl.tudelft.ipv8.IPv4Address
import nl.tudelft.ipv8.Peer
import nl.tudelft.ipv8.messaging.Packet
import nl.tudelft.ipv8.messaging.payload.IntroductionResponsePayload
import nl.tudelft.trustchain.atomicswap.messages.*
import nl.tudelft.trustchain.atomicswap.ui.swap.LOG
import java.util.*

typealias onAccept = (AcceptMessage, Peer) -> Unit
typealias onInitiate = (InitiateMessage, Peer) -> Unit
typealias onTrade = (TradeMessage, Peer) -> Unit
typealias onComplete = (CompleteSwapMessage, Peer) -> Unit
typealias onRemove = (RemoveTradeMessage, Peer) -> Unit

object AtomicSwapTrustchainConstants {
    const val ATOMIC_SWAP_COMPLETED_BLOCK = "atomic_swap_completed_block"
    const val TRANSACTION_FROM_COIN = "from_coin"
    const val TRANSACTION_TO_COIN = "to_coin"
    const val TRANSACTION_FROM_AMOUNT = "from_amount"
    const val TRANSACTION_TO_AMOUNT = "to_amount"
    const val TRANSACTION_OFFER_ID = "offer_id"
}

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
        messageHandlers[Companion.REMOVE_MESSAGE_ID] = ::onRemoveMessage
    }


    private lateinit var onAcceptCallback: onAccept
    private lateinit var onInitiateCallback: onInitiate
    private lateinit var onTradeCallback: onTrade
    private lateinit var onCompleteCallback: onComplete
    private lateinit var onRemoveCallback: onRemove

    fun setOnAccept(callback: onAccept) = callback.also {
        onAcceptCallback = it
    }

    fun setOnInitiate(callback: onInitiate) = callback.also {
        onInitiateCallback = it
    }
    fun setOnTrade(callback: onTrade) = callback.also {
        onTradeCallback = it
    }
    fun setOnComplete(callback: onComplete) = callback.also {
        onCompleteCallback = it
    }
    fun setOnRemove(callback: onRemove) = callback.also {
        onRemoveCallback = it
    }



    // Functions for receiving messages
    private fun onTradeOfferMessage(packet: Packet) {
        val (peer, payload) = packet.getAuthPayload(TradeMessage.Deserializer)
        Log.d("AtomicSwapCommunity", peer.mid + ":PAYLOAD: " + payload.offerId)
        onTradeCallback(payload, peer)
    }


    private fun onAcceptMessage(packet: Packet) {
        val (peer, payload) = packet.getAuthPayload(AcceptMessage.Deserializer)
        Log.d("AtomicSwapCommunity", peer.mid + ": got trade accept ")
        onAcceptCallback(payload, peer)
    }

    private fun onInitiateMessage(packet: Packet) {
        val (peer, payload) = packet.getAuthPayload(InitiateMessage.Deserializer)
        /* Create and broadcast tx script */
        onInitiateCallback(payload, peer)
    }

    private fun onCompleteTrade(packet: Packet) {
        try {
            val (peer, payload) = packet.getAuthPayload(CompleteSwapMessage.Deserializer)
            onCompleteCallback(payload, peer)
            Log.d("AtomicSwapCommunity", peer.mid + ": TRADE COMPLETED " + payload.offerId)
        } catch (e:Exception){
            Log.e(LOG, "Error occurred when completing trade", e)
        }
    }

    private fun onRemoveMessage(packet: Packet) {
        val (peer, payload) = packet.getAuthPayload(RemoveTradeMessage.Deserializer)
        Log.d("AtomicSwapCommunity", "Received remove tade message")
        onRemoveCallback(payload, peer)
    }

    // Function for sending messages
    fun broadcastTradeOffer(offerId: String, fromCoin: String, toCoin: String, fromAmount: String, toAmount: String) {
        for (peer in getPeers()) {
            val packet = serializePacket(
                Companion.BROADCAST_TRADE_MESSAGE_ID, TradeMessage(offerId,
                    fromCoin,
                    toCoin,
                    fromAmount,
                    toAmount)
            )
            send(peer.address, packet)
        }
    }

    fun sendAcceptMessage(peer: Peer, offerId:String, btcPubKey: String, ethAddress: String ){
        send(
            peer.address,
            serializePacket(Companion.ACCEPT_MESSAGE_ID, AcceptMessage(offerId, btcPubKey, ethAddress))
        )
    }

    fun sendInitiateMessage(peer:Peer, offerId: String, data: OnAcceptReturn) {
        Log.d("AtomicSwapCommunity", peer.mid + ": SENDING INITIATE")
        send(
            peer.address,
            serializePacket(
                Companion.INITIATE_MESSAGE_ID,
                InitiateMessage(offerId, data.secretHash, data.txId, data.btcPubKey,data.ethAddress)
            )
        )
    }

    fun sendCompleteMessage(peer: Peer, offerId: String, txId: String){
        Log.d("AtomicSwapCommunity", peer.mid + ": SENDING COMPLETE MESSAGE")
        send(
            peer.address,
            serializePacket(
                Companion.COMPLETE_MESSAGE_ID,
                CompleteSwapMessage(offerId, txId)
            )
        )
    }

    fun sendRemoveTradeMessage(offerId: String) {
        Log.d("AtomicSwapCommunity", "SENDING REMOVE TRADE MESS")
        for (peer in getPeers()) {
            send(
                peer.address,
                serializePacket(
                    Companion.REMOVE_MESSAGE_ID,
                    RemoveTradeMessage(offerId)
                )
            )
        }
    }


    companion object {
        private const val BROADCAST_TRADE_MESSAGE_ID = 1
        private const val ACCEPT_MESSAGE_ID = 2
        private const val INITIATE_MESSAGE_ID = 3
        private const val COMPLETE_MESSAGE_ID = 4
        private const val REMOVE_MESSAGE_ID = 5
    }
}

/**
 * @param hash: hash of the secret needed to claim the swap.
 */
data class OnAcceptReturn(
    val secretHash: String,
    val txId: String,
    val btcPubKey: String,
    val ethAddress: String
)
