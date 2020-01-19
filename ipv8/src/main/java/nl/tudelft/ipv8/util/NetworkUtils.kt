package nl.tudelft.ipv8.util

import android.util.Log
import nl.tudelft.ipv8.Address
import java.net.Inet4Address
import java.net.InetAddress
import java.net.NetworkInterface
import java.nio.ByteBuffer

/**
 * Returns true if the address is located within a subnet of one of our network interfaces.
 */
fun addressIsLan(address: Address): Boolean {
    val interfaces = NetworkInterface.getNetworkInterfaces()
    for (intf in interfaces) {
        for (intfAddr in intf.interfaceAddresses) {
            if (intfAddr.address is Inet4Address && !intfAddr.address.isLoopbackAddress) {
                val inetAddr = InetAddress.getByName(address.ip)
                return addressInSubnet(inetAddr, intfAddr.address, intfAddr.networkPrefixLength)
            }
        }
    }
    return false
}

fun addressInSubnet(address: InetAddress, subnetAddress: InetAddress, networkPrefixLength: Short): Boolean {
    val mask = 0xFFFFFF shl (32 - networkPrefixLength)
    val intfAddrInt = ByteBuffer.wrap(subnetAddress.address).int
    val targetAddrInt = ByteBuffer.wrap(address.address).int
    val inSubnet = intfAddrInt and mask == targetAddrInt and mask
    Log.d("NetworkUtils", "addr:$subnetAddress, mask:$mask, $intfAddrInt $targetAddrInt -> ${intfAddrInt and mask} == ${targetAddrInt and mask} -> $inSubnet")
    return inSubnet
}
