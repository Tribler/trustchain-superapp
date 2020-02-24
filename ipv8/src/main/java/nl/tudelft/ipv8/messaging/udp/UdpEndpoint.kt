package nl.tudelft.ipv8.messaging.udp

import kotlinx.coroutines.*
import mu.KotlinLogging
import nl.tudelft.ipv8.Address
import nl.tudelft.ipv8.messaging.Endpoint
import nl.tudelft.ipv8.messaging.Packet
import java.io.IOException
import java.net.*

private val logger = KotlinLogging.logger {}

open class UdpEndpoint(
    private val port: Int,
    private val ip: InetAddress
) : Endpoint() {
    private var socket: DatagramSocket? = null
    private var bindThread: BindThread? = null

    private val job = SupervisorJob()
    private val scope = CoroutineScope(Dispatchers.IO + job)

    override fun isOpen(): Boolean {
        return socket?.isBound == true
    }

    override fun send(address: Address, data: ByteArray) {
        if (!isOpen()) throw IllegalStateException("UDP socket is closed")

        scope.launch {
            logger.debug("Send packet (${data.size} B) to $address")
            try {
                val datagramPacket = DatagramPacket(data, data.size, address.toSocketAddress())
                withContext(Dispatchers.IO) {
                    socket?.send(datagramPacket)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    override fun open() {
        val socket = getDatagramSocket()
        this.socket = socket

        logger.info { "Opened UDP socket on port ${socket.localPort}" }

        startLanEstimation()

        val bindThread = BindThread(socket)
        bindThread.start()
        this.bindThread = bindThread
    }

    /**
     * Finds the nearest unused socket.
     */
    private fun getDatagramSocket(): DatagramSocket {
        for (i in 0 until 100) {
            try {
                return DatagramSocket(port + i, ip)
            } catch (e: Exception) {
                // Try another port
            }
        }
        throw IllegalStateException("No unused socket found")
    }

    override fun close() {
        if (!isOpen()) throw IllegalStateException("UDP socket is already closed")

        stopLanEstimation()

        socket?.close()
        socket = null

        bindThread = null
    }

    open fun startLanEstimation() {
        val interfaces = NetworkInterface.getNetworkInterfaces()
        for (intf in interfaces) {
            for (intfAddr in intf.interfaceAddresses) {
                if (intfAddr.address is Inet4Address && !intfAddr.address.isLoopbackAddress) {
                    val estimatedAddress = Address(intfAddr.address.hostAddress, getSocketPort())
                    setEstimatedLan(estimatedAddress)
                }
            }
        }
    }

    open fun stopLanEstimation() {
    }

    protected fun getSocketPort(): Int {
        return socket?.localPort ?: port
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
                        "Received packet (${receivePacket.length} B) from $sourceAddress"
                    )
                    notifyListeners(packet)
                }
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }
    }
}
