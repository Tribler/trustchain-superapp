package nl.tudelft.ipv8.peerdiscovery

import nl.tudelft.ipv8.Address
import nl.tudelft.ipv8.Peer
import nl.tudelft.ipv8.keyvault.JavaCryptoProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class NetworkTest {
    @Test
    fun discoverAddress_newPeer_newAddress() {
        val network = Network()
        val peer = Peer(JavaCryptoProvider.generateKey())
        val address = Address("1.2.3.4", 1234)
        val serviceId = "123"
        network.discoverAddress(peer, address, serviceId)
        assertEquals(Pair(peer.mid, serviceId), network.allAddresses[address])
    }

    @Test
    fun discoverAddress_blacklist() {
        val network = Network()
        val peer = Peer(JavaCryptoProvider.generateKey())
        val address = Address("1.2.3.4", 1234)
        network.blacklist.add(address)
        val serviceId = "123"
        network.discoverAddress(peer, address, serviceId)
        assertEquals(null, network.allAddresses[address])
    }

    @Test
    fun getServicesForPeer() {
        val network = Network()
        val peer = Peer(JavaCryptoProvider.generateKey())
        val serviceId = "123"
        network.discoverServices(peer, listOf(serviceId))
        val services = network.getServicesForPeer(peer)
        assertEquals(1, services.size)
        assertEquals(serviceId, services.first())
    }

    @Test
    fun getPeersForService() {
        val network = Network()
        val peer = Peer(JavaCryptoProvider.generateKey())
        val serviceId = "123"
        network.addVerifiedPeer(peer)
        network.discoverServices(peer, listOf(serviceId))
        val peers = network.getPeersForService(serviceId)
        assertEquals(1, peers.size)
        assertEquals(peer, peers.first())
    }

    @Test
    fun getVerifiedByPublicKeyBin() {
        val network = Network()
        val peer = Peer(JavaCryptoProvider.generateKey())
        network.addVerifiedPeer(peer)
        val verifiedPeer = network.getVerifiedByPublicKeyBin(peer.publicKey.keyToBin())
        assertEquals(peer.mid, verifiedPeer?.mid)
    }

    @Test
    fun getIntroductionFrom() {
        val network = Network()
        val peer = Peer(JavaCryptoProvider.generateKey(), Address("1.2.3.4", 1234))
        val introducedAddress = Address("2.3.4.5", 2345)
        network.discoverAddress(peer, introducedAddress)
        val introductions = network.getIntroductionFrom(peer)
        assertEquals(1, introductions.size)
        assertEquals(introducedAddress, introductions[0])
    }

    @Test
    fun removeByAddress() {
        val network = Network()
        val address = Address("1.2.3.4", 1234)
        val peer = Peer(JavaCryptoProvider.generateKey(), address)
        network.addVerifiedPeer(peer)
        network.removeByAddress(address)
        val verifiedPeer = network.getVerifiedByAddress(address)
        assertNull(verifiedPeer)
    }

    @Test
    fun removePeer() {
        val network = Network()
        val address = Address("1.2.3.4", 1234)
        val peer = Peer(JavaCryptoProvider.generateKey(), address)
        network.addVerifiedPeer(peer)
        network.removePeer(peer)
        val verifiedPeer = network.getVerifiedByAddress(address)
        assertNull(verifiedPeer)
    }

    @Test
    fun getWalkableAddresses() {
        val network = Network()

        val noAddresses = network.getWalkableAddresses(null)
        assertEquals(0, noAddresses.size)

        val address = Address("1.2.3.4", 1234)
        val peer = Peer(JavaCryptoProvider.generateKey(), address)
        network.addVerifiedPeer(peer)

        // all peers are known
        val noWalkableAddresses = network.getWalkableAddresses(null)
        assertEquals(0, noWalkableAddresses.size)

        val walkableAddresses = network.getWalkableAddresses("abc")
        assertEquals(1, walkableAddresses.size)
    }

    @Test
    fun getRandomPeer_null() {
        val network = Network()
        val randomPeer = network.getRandomPeer()
        assertNull(randomPeer)
    }

    @Test
    fun getRandomPeers_empty() {
        val network = Network()
        val randomPeers = network.getRandomPeers(1)
        assertEquals(0, randomPeers.size)
    }

    @Test
    fun getRandomPeer_notNull() {
        val network = Network()

        val address = Address("1.2.3.4", 1234)
        val peer = Peer(JavaCryptoProvider.generateKey(), address)
        network.addVerifiedPeer(peer)

        val randomPeer = network.getRandomPeer()
        assertEquals(peer, randomPeer)
    }

    @Test
    fun getRandomPeers_notEmpty() {
        val network = Network()

        val address = Address("1.2.3.4", 1234)
        val peer = Peer(JavaCryptoProvider.generateKey(), address)
        network.addVerifiedPeer(peer)

        val randomPeers = network.getRandomPeers(1)
        assertEquals(1, randomPeers.size)
        assertEquals(peer, randomPeers[0])
    }
}
