package nl.tudelft.ipv8.android.messaging.udp

import android.net.ConnectivityManager
import android.net.LinkProperties
import android.net.Network
import mu.KotlinLogging
import nl.tudelft.ipv8.Address
import nl.tudelft.ipv8.messaging.udp.UdpEndpoint
import java.net.*

private val logger = KotlinLogging.logger {}

class AndroidUdpEndpoint(
    port: Int,
    ip: InetAddress,
    private val connectivityManager: ConnectivityManager
) : UdpEndpoint(port, ip) {

    private val defaultNetworkCallback = object : ConnectivityManager.NetworkCallback() {
        override fun onLinkPropertiesChanged(network: Network, linkProperties: LinkProperties) {
            super.onLinkPropertiesChanged(network, linkProperties)
            logger.debug("onLinkPropertiesChanged " + linkProperties.linkAddresses)
            for (linkAddress in linkProperties.linkAddresses) {
                if (linkAddress.address is Inet4Address && !linkAddress.address.isLoopbackAddress) {
                    val estimatedAddress = Address(linkAddress.address.hostAddress, getSocketPort())
                    setEstimatedLan(estimatedAddress)
                }
            }
        }
    }

    override fun startLanEstimation() {
        connectivityManager.registerDefaultNetworkCallback(defaultNetworkCallback)
    }

    override fun stopLanEstimation() {
        connectivityManager.unregisterNetworkCallback(defaultNetworkCallback)
    }
}
