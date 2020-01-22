package nl.tudelft.ipv8.util

import nl.tudelft.ipv8.Address
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.net.InetAddress

class NetworkUtilsTest {
    @Test
    fun addressIsLan_simple() {
        assertFalse(addressIsLan(Address("1.2.3.4", 0)))
    }

    @Test
    fun addressInSubnet_true() {
        val address = InetAddress.getByName("192.168.1.31")
        val subnetAddress = InetAddress.getByName("192.168.1.31")
        val networkPrefixLength = 24
        assertTrue(addressInSubnet(address, subnetAddress, networkPrefixLength.toShort()))
    }

    @Test
    fun addressInSubnet_false() {
        val address = InetAddress.getByName("192.168.1.31")
        val subnetAddress = InetAddress.getByName("192.168.2.31")
        val networkPrefixLength = 24
        assertFalse(addressInSubnet(address, subnetAddress, networkPrefixLength.toShort()))
    }
}
