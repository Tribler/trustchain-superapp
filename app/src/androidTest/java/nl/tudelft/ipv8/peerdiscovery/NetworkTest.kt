package nl.tudelft.ipv8.peerdiscovery

import androidx.test.ext.junit.runners.AndroidJUnit4
import nl.tudelft.ipv8.Address
import nl.tudelft.ipv8.Peer
import nl.tudelft.ipv8.keyvault.ECCrypto
import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class NetworkTest {
    @Test
    fun discoverAddress_newPeer_newAddress() {
        val network = Network()
        val peer = Peer(ECCrypto.generateKey())
        val address = Address("1.2.3.4", 1234)
        val serviceId = "123"
        network.discoverAddress(peer, address, serviceId)
        Assert.assertEquals(Pair(peer.mid, serviceId), network.allAddresses[address])
        Assert.assertEquals(setOf(address), network.reverseIntroLookup[peer])
    }

    @Test
    fun getServicesForPeer() {
        val network = Network()
        val peer = Peer(ECCrypto.generateKey())
        val serviceId = "123"
        network.discoverServices(peer, listOf(serviceId))
        val services = network.getServicesForPeer(peer)
        Assert.assertEquals(1, services.size)
        Assert.assertEquals(serviceId, services.first())
    }

    @Test
    fun getPeersForService() {
        val network = Network()
        val peer = Peer(ECCrypto.generateKey())
        val serviceId = "123"
        network.addVerifiedPeer(peer)
        network.discoverServices(peer, listOf(serviceId))
        val peers = network.getPeersForService(serviceId)
        Assert.assertEquals(1, peers.size)
        Assert.assertEquals(peer, peers.first())
    }
}
