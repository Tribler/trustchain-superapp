package nl.tudelft.ipv8.messaging.udp

import nl.tudelft.ipv8.Address
import nl.tudelft.ipv8.messaging.Endpoint
import nl.tudelft.ipv8.messaging.Packet
import java.io.IOException
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.net.SocketAddress

class UdpEndpoint(
    private val port: Int,
    private val ip: InetAddress
) : Endpoint() {
    private var socket: DatagramSocket? = null
    private var bindThread: BindThread? = null

    override fun isOpen(): Boolean {
        return socket?.isBound == true
    }

    override fun send(socketAddress: SocketAddress, data: ByteArray) {
        assert(isOpen()) { "UDP socket is closed" }
        val datagramPacket = DatagramPacket(data, data.size, socketAddress)
        socket?.send(datagramPacket)
    }

    override fun open() {
        val socket = DatagramSocket(port, ip)
        this.socket = socket

        val bindThread = BindThread(socket)
        bindThread.start()
        this.bindThread = bindThread
    }

    override fun close() {
        socket?.disconnect()
        socket = null
    }

    inner class BindThread(
        private val socket: DatagramSocket
    ): Thread() {
        override fun run() {
            try {
                val receiveData = ByteArray(1500)
                while (isAlive) {
                    val receivePacket = DatagramPacket(receiveData, receiveData.size)
                    socket.receive(receivePacket)
                    val sourceAddress = Address(receivePacket.address.hostAddress, receivePacket.port)
                    val packet = Packet(sourceAddress, receivePacket.data)
                    notifyListeners(packet)
                }
            } catch (e: IOException) {
                // TODO: handle errors
                e.printStackTrace()
            }
        }
    }
}
