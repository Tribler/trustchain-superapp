package nl.tudelft.trustchain.common

import android.util.Log
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.BroadcastChannel
import kotlinx.coroutines.channels.Channel
import nl.tudelft.ipv8.IPv4Address
import nl.tudelft.ipv8.Community
import nl.tudelft.ipv8.Peer
import nl.tudelft.ipv8.android.IPv8Android
import nl.tudelft.ipv8.attestation.trustchain.TrustChainCommunity
import nl.tudelft.ipv8.messaging.*
import nl.tudelft.ipv8.messaging.payload.IntroductionResponsePayload
import nl.tudelft.ipv8.messaging.payload.PuncturePayload
import java.util.*

class DemoCommunity : Community() {
    override val serviceId = "02313685c1912a141279f8248fc8db5899c5df5a"

    val discoveredAddressesContacted: MutableMap<IPv4Address, Date> = mutableMapOf()

    val lastTrackerResponses = mutableMapOf<IPv4Address, Date>()

    @ExperimentalCoroutinesApi
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

    private var torrentMessagesList = ArrayList<Packet>()

    public fun getTorrentMessages(): ArrayList<Packet> {
        return torrentMessagesList
    }

    object MessageId {
        const val THALIS_MESSAGE = 222
        const val TORRENT_MESSAGE = 223
        const val PUNCTURE_TEST = 251
    }

    // SEND MESSAGE
    fun broadcastGreeting() {
        for (peer in getPeers()) {
            val packet = serializePacket(MessageId.THALIS_MESSAGE, MyMessage("Hello from Freedom of Computing!"), true)
            send(peer.address, packet)
        }
    }

    fun informAboutTorrent(torrentName: String) {
        if (torrentName != "") {
            for (peer in getPeers()) {
                val packet = serializePacket(
                    MessageId.TORRENT_MESSAGE,
                    MyMessage("FOC:" + torrentName),
                    true
                )
                send(peer.address, packet)
            }
        }
    }

    fun sendPuncture(address: IPv4Address, id: Int) {
        val payload = PuncturePayload(myEstimatedLan, myEstimatedWan, id)
        val packet = serializePacket(MessageId.PUNCTURE_TEST, payload, sign = false)
        endpoint.send(address, packet)
    }

    // RECEIVE MESSAGE
    init {
        messageHandlers[MessageId.THALIS_MESSAGE] = ::onMessage
        messageHandlers[MessageId.TORRENT_MESSAGE] = ::onTorrentMessage
        messageHandlers[MessageId.PUNCTURE_TEST] = ::onPunctureTest
    }

    private fun onMessage(packet: Packet) {
        val (peer, payload) = packet.getAuthPayload(MyMessage.Deserializer)
        Log.i("personal", peer.mid + ": " + payload.message)
    }

    private fun onTorrentMessage(packet: Packet) {
        torrentMessagesList.add(packet)
        val (peer, payload) = packet.getAuthPayload(MyMessage.Deserializer)
        Log.i("personal", peer.mid + ": " + payload.message)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    private fun onPunctureTest(packet: Packet) {
        val payload = packet.getPayload(PuncturePayload.Deserializer)
        punctureChannel.offer(Pair(packet.source, payload))
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
