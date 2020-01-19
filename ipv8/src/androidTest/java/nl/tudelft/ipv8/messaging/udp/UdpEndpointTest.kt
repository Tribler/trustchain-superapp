package nl.tudelft.ipv8.messaging.udp

import android.content.Context
import android.net.ConnectivityManager
import androidx.test.platform.app.InstrumentationRegistry
import io.mockk.mockk
import nl.tudelft.ipv8.Address
import nl.tudelft.ipv8.messaging.EndpointListener
import org.junit.Assert.*
import org.junit.Test
import java.net.InetAddress

class UdpEndpointTest {
    @Test
    fun openAndClose() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val endpoint = UdpEndpoint(1234, InetAddress.getByName("0.0.0.0"), connectivityManager)
        assertFalse(endpoint.isOpen())
        endpoint.open()
        assertTrue(endpoint.isOpen())
        endpoint.close()
        assertFalse(endpoint.isOpen())
    }

    @Test
    fun sendAndReceive() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val endpoint = UdpEndpoint(1234, InetAddress.getByName("0.0.0.0"), connectivityManager)
        val listener = mockk<EndpointListener>(relaxed = true)
        endpoint.addListener(listener)

        endpoint.open()
        val data = "Hello world!".toByteArray(Charsets.US_ASCII)
        endpoint.send(Address("0.0.0.0", 1234), data)

        endpoint.close()
    }
}
