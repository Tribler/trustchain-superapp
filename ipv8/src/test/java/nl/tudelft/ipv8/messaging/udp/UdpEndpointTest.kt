package nl.tudelft.ipv8.messaging.udp

import io.mockk.mockk
import nl.tudelft.ipv8.Address
import nl.tudelft.ipv8.messaging.EndpointListener
import org.junit.Assert.*
import org.junit.Test
import java.net.InetAddress

class UdpEndpointTest {
    @Test
    fun openAndClose() {
        val endpoint = UdpEndpoint(1234, InetAddress.getByName("0.0.0.0"))
        assertFalse(endpoint.isOpen())
        endpoint.open()
        assertTrue(endpoint.isOpen())
        endpoint.close()
        assertFalse(endpoint.isOpen())
    }

    @Test
    fun sendAndReceive() {
        val endpoint = UdpEndpoint(1234, InetAddress.getByName("0.0.0.0"))
        val listener = mockk<EndpointListener>(relaxed = true)
        endpoint.addListener(listener)

        endpoint.open()
        val data = "Hello world!".toByteArray(Charsets.US_ASCII)
        endpoint.send(Address("0.0.0.0", 1234), data)

        endpoint.close()
    }
}
