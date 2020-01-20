package nl.tudelft.ipv8.messaging.udp

import android.net.ConnectivityManager
import android.net.LinkProperties
import android.net.Network
import android.os.Handler
import android.os.HandlerThread
import android.util.Log
import nl.tudelft.ipv8.Address
import nl.tudelft.ipv8.messaging.Endpoint
import nl.tudelft.ipv8.messaging.Packet
import nl.tudelft.ipv8.util.toHex
import java.io.IOException
import java.net.*

class UdpEndpoint(
    private val port: Int,
    private val ip: InetAddress,
    private val connectivityManager: ConnectivityManager
) : Endpoint() {
    private var socket: DatagramSocket? = null
    private var bindThread: BindThread? = null
    private var sendThread: HandlerThread? = null
    private var sendHandler: Handler? = null

    private val defaultNetworkCallback = object : ConnectivityManager.NetworkCallback() {
        override fun onLinkPropertiesChanged(network: Network, linkProperties: LinkProperties) {
            super.onLinkPropertiesChanged(network, linkProperties)
            Log.d("UdpEndpoint", "onLinkPropertiesChanged " + linkProperties.linkAddresses)
            for (linkAddress in linkProperties.linkAddresses) {
                if (linkAddress.address is Inet4Address) {
                    val estimatedAddress = Address(linkAddress.address.hostAddress, port)
                    Log.d("UdpEndpoint", "estimated address: $estimatedAddress")
                    setEstimatedLan(estimatedAddress)
                }
            }
        }
    }

    override fun isOpen(): Boolean {
        return socket?.isBound == true
    }

    override fun send(address: Address, data: ByteArray) {
        assert(isOpen()) { "UDP socket is closed" }
        sendHandler?.post {
            Log.d("UdpEndpoint", "send packet (${data.size} B) to $address")
            Log.d("UdpEndpoint", data.toHex())
            try {
                val datagramPacket = DatagramPacket(data, data.size, address.toSocketAddress())
                socket?.send(datagramPacket)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    override fun open() {
        connectivityManager.registerDefaultNetworkCallback(defaultNetworkCallback)

        val sendThread = HandlerThread("SendThread")
        sendThread.start()
        this.sendThread = sendThread
        sendHandler = Handler(sendThread.looper)

        val socket = DatagramSocket(port, ip)
        this.socket = socket

        val bindThread = BindThread(socket)
        bindThread.start()
        this.bindThread = bindThread
    }

    override fun close() {
        connectivityManager.unregisterNetworkCallback(defaultNetworkCallback)

        sendThread?.quit()
        sendHandler?.removeCallbacksAndMessages(null)
        socket?.close()
        socket = null
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
                    Log.d(
                        "UdpEndpoint",
                        "received packet (${receivePacket.length} B) from $sourceAddress"
                    )
                    Log.d("UdpEndpoint", packet.data.toHex())
                    notifyListeners(packet)
                }
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }
    }
}
