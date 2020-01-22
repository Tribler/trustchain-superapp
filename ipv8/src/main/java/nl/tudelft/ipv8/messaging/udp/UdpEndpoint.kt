package nl.tudelft.ipv8.messaging.udp

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import mu.KotlinLogging
import nl.tudelft.ipv8.Address
import nl.tudelft.ipv8.messaging.Endpoint
import nl.tudelft.ipv8.messaging.Packet
import nl.tudelft.ipv8.util.toHex
import java.io.IOException
import java.net.*

private val logger = KotlinLogging.logger {}

open class UdpEndpoint(
    private val port: Int,
    private val ip: InetAddress
) : Endpoint() {
    private var socket: DatagramSocket? = null
    private var bindThread: BindThread? = null

    private val scope = CoroutineScope(Dispatchers.IO)

    override fun isOpen(): Boolean {
        return socket?.isBound == true
    }

    override fun send(address: Address, data: ByteArray) {
        assert(isOpen()) { "UDP socket is closed" }
        scope.launch {
            logger.debug("send packet (${data.size} B) to $address")
            logger.debug(data.toHex())
            try {
                val datagramPacket = DatagramPacket(data, data.size, address.toSocketAddress())
                socket?.send(datagramPacket)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    override fun open() {
        startLanEstimation()

        val socket = DatagramSocket(port, ip)
        this.socket = socket

        val bindThread = BindThread(socket)
        bindThread.start()
        this.bindThread = bindThread
    }

    override fun close() {
        stopLanEstimation()

        socket?.close()
        socket = null

        scope.cancel()
    }

    open fun startLanEstimation() {
        // TODO
    }

    open fun stopLanEstimation() {
        // TODO
    }

    inner class BindThread(
        private val socket: DatagramSocket
    ) : Thread() {
        override fun run() {
            try {
                val receiveData = ByteArray(1500)
                while (isAlive) {
                    val receivePacket = DatagramPacket(receiveData, receiveData.size)
                    socket.receive(receivePacket)
                    val sourceAddress =
                        Address(receivePacket.address.hostAddress, receivePacket.port)
                    val packet =
                        Packet(sourceAddress, receivePacket.data.copyOf(receivePacket.length))
                    logger.debug(
                        "received packet (${receivePacket.length} B) from $sourceAddress"
                    )
                    notifyListeners(packet)
                }
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }
    }
}
