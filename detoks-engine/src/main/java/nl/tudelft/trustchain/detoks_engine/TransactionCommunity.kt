package nl.tudelft.trustchain.detoks_engine

import TestMessage
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import mu.KotlinLogging
import nl.tudelft.ipv8.Community
import nl.tudelft.ipv8.IPv4Address
import nl.tudelft.ipv8.Peer
import nl.tudelft.ipv8.messaging.Packet
import nl.tudelft.ipv8.messaging.udp.UdpEndpoint
import java.io.ByteArrayOutputStream
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.locks.ReentrantLock

private const val MESSAGE_ID = 1
private val logger = KotlinLogging.logger {}

class TransactionCommunity : Community() {
    override val serviceId = "02315685d1932a144279f8248fc3db5899c5df8c"
    private var handler: (msg: String) -> Unit = logger::debug

    init {
        messageHandlers[MESSAGE_ID] = ::onMessage
    }

    fun setHandler(onMsg: (msg: String) -> Unit) {
        handler = onMsg
    }

    private fun onMessage(packet: Packet) {
        val (peer, payload) = packet.getAuthPayload(TestMessage.Deserializer)
        logger.debug("DemoCommunity", peer.mid + ": " + payload.message)
        handler("DemoCommunity, ${peer.mid} : ${payload.message}")
    }

    fun send(peer: Peer, token: String) {
        val packet = serializePacket(MESSAGE_ID, TestMessage(token))
        send(peer.address, packet)
    }

    fun broadcastGreeting() {
        for (peer in getPeers()) {
            val packet = serializePacket(
                MESSAGE_ID,
                TestMessage("Hello!")
            )
            send(peer.address, packet)
        }
    }


    private val sendBufferMap = ConcurrentHashMap<IPv4Address, SendBuffer>()

    fun sendAsync(peer: Peer, token: String) = scope.launch(Dispatchers.IO) {
        val addr = peer.address
        val packet = serializePacket(MESSAGE_ID, TestMessage(token))
        val buf = sendBufferMap.getOrPut(addr) { SendBuffer() }
        buf.lock()
        try {
            if (buf.size() + packet.size > UdpEndpoint.UDP_PAYLOAD_LIMIT) {
                if (buf.size() > 0) {
                    // the call starts a coroutine internally, thus not blocking
                    send(addr, buf.toByteArray())
                    Log.d("detoks_engine", "sent ${buf.size()} bytes of data to $addr")
                    buf.reset()
                }
            }
            buf.write(packet)
            Log.d("detoks_engine", "buffered ${packet.size} bytes of data for $addr")
        } finally {
            buf.unlock()
        }
    }

    fun startAsyncSender() = scope.launch {
        while (isActive) {
            delay(50)
            for ((addr, buf) in sendBufferMap) {
                launch {
                    buf.lock()
                    try {
                        if (buf.size() > 0) {
                            send(addr, buf.toByteArray())
                            Log.d("detoks_engine", "sent ${buf.size()} bytes of data to $addr")
                            buf.reset()
                        }
                    } finally {
                        buf.unlock()
                    }
                }
            }
        }
    }
}


private class SendBuffer {
    private val buffer = ByteArrayOutputStream()

    private val lock = ReentrantLock()

    fun size() = buffer.size()

    fun toByteArray(): ByteArray = buffer.toByteArray()

    fun write(b: ByteArray) = buffer.write(b)

    fun reset() = buffer.reset()

    fun lock() = lock.lock()

    fun unlock() = lock.unlock()
}
