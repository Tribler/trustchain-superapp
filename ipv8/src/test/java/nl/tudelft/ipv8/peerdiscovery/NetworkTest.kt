package nl.tudelft.ipv8.peerdiscovery

import nl.tudelft.ipv8.Address
import nl.tudelft.ipv8.Peer
import nl.tudelft.ipv8.keyvault.JavaCryptoProvider
import org.junit.Assert
import org.junit.Test

class NetworkTest {
    @Test
    fun discoverAddress_newPeer_newAddress() {
        val network = Network()
        val peer = Peer(JavaCryptoProvider.generateKey())
        val address = Address("1.2.3.4", 1234)
        val serviceId = "123"
        network.discoverAddress(peer, address, serviceId)
        Assert.assertEquals(Pair(peer.mid, serviceId), network.allAddresses[address])
    }

    @Test
    fun discoverAddress_blacklist() {
        val network = Network()
        val peer = Peer(JavaCryptoProvider.generateKey())
        val address = Address("1.2.3.4", 1234)
        network.blacklist.add(address)
        val serviceId = "123"
        network.discoverAddress(peer, address, serviceId)
        Assert.assertEquals(null, network.allAddresses[address])
    }

    @Test
    fun getServicesForPeer() {
        val network = Network()
        val peer = Peer(JavaCryptoProvider.generateKey())
        val serviceId = "123"
        network.discoverServices(peer, listOf(serviceId))
        val services = network.getServicesForPeer(peer)
        Assert.assertEquals(1, services.size)
        Assert.assertEquals(serviceId, services.first())
    }

    @Test
    fun getPeersForService() {
        val network = Network()
        val peer = Peer(JavaCryptoProvider.generateKey())
        val serviceId = "123"
        network.addVerifiedPeer(peer)
        network.discoverServices(peer, listOf(serviceId))
        val peers = network.getPeersForService(serviceId)
        Assert.assertEquals(1, peers.size)
        Assert.assertEquals(peer, peers.first())
    }

    @Test
    fun getVerifiedByPublicKeyBin() {
        val network = Network()
        val peer = Peer(JavaCryptoProvider.generateKey())
        network.addVerifiedPeer(peer)
        val verifiedPeer = network.getVerifiedByPublicKeyBin(peer.publicKey.keyToBin())
        Assert.assertEquals(peer.mid, verifiedPeer?.mid)
    }

    @Test
    fun getIntroductionFrom() {
        val network = Network()
        val peer = Peer(JavaCryptoProvider.generateKey(), Address("1.2.3.4", 1234))
        val introducedAddress = Address("2.3.4.5", 2345)
        network.discoverAddress(peer, introducedAddress)
        val introductions = network.getIntroductionFrom(peer)
        Assert.assertEquals(1, introductions.size)
        Assert.assertEquals(introducedAddress, introductions[0])
    }

    @Test
    fun removeByAddress() {
        val network = Network()
        val address = Address("1.2.3.4", 1234)
        val peer = Peer(JavaCryptoProvider.generateKey(), address)
        network.addVerifiedPeer(peer)
        network.removeByAddress(address)
        val verifiedPeer = network.getVerifiedByAddress(address)
        Assert.assertNull(verifiedPeer)
    }

    @Test
    fun removePeer() {
        val network = Network()
        val address = Address("1.2.3.4", 1234)
        val peer = Peer(JavaCryptoProvider.generateKey(), address)
        network.addVerifiedPeer(peer)
        network.removePeer(peer)
        val verifiedPeer = network.getVerifiedByAddress(address)
        Assert.assertNull(verifiedPeer)
    }

    @Test
    fun getWalkableAddresses() {
        val network = Network()

        val noAddresses = network.getWalkableAddresses(null)
        Assert.assertEquals(0, noAddresses.size)

        val address = Address("1.2.3.4", 1234)
        val peer = Peer(JavaCryptoProvider.generateKey(), address)
        network.discoverAddress(peer, address, "abc")

        val walkableAddresses = network.getWalkableAddresses("abc")
        Assert.assertEquals(1, walkableAddresses.size)

        network.addVerifiedPeer(peer)

        // all peers are known
        val noWalkableAddresses = network.getWalkableAddresses(null)
        Assert.assertEquals(0, noWalkableAddresses.size)
    }

    @Test
    fun getRandomPeer_null() {
        val network = Network()
        val randomPeer = network.getRandomPeer()
        Assert.assertNull(randomPeer)
    }

    @Test
    fun getRandomPeers_empty() {
        val network = Network()
        val randomPeers = network.getRandomPeers(1)
        Assert.assertEquals(0, randomPeers.size)
    }

    @Test
    fun getRandomPeer_notNull() {
        val network = Network()

        val address = Address("1.2.3.4", 1234)
        val peer = Peer(JavaCryptoProvider.generateKey(), address)
        network.addVerifiedPeer(peer)

        val randomPeer = network.getRandomPeer()
        Assert.assertEquals(peer, randomPeer)
    }

    @Test
    fun getRandomPeers_notEmpty() {
        val network = Network()

        val address = Address("1.2.3.4", 1234)
        val peer = Peer(JavaCryptoProvider.generateKey(), address)
        network.addVerifiedPeer(peer)

        val randomPeers = network.getRandomPeers(1)
        Assert.assertEquals(1, randomPeers.size)
        Assert.assertEquals(peer, randomPeers[0])
    }

    @Test
    fun addPeer_retainAddresses() {
        val network = Network()
        val key = JavaCryptoProvider.generateKey()
        val address = Address("1.2.3.4", 1234)
        val lanAddress = Address("2.3.4.5", 234)
        val wanAddress = Address("3.4.5.6", 3456)
        val peer = Peer(key, address, lanAddress, wanAddress)
        network.addVerifiedPeer(peer)

        val newPeer = Peer(key, address)
        network.addVerifiedPeer(newPeer)

        val probablePeer = network.getVerifiedByPublicKeyBin(key.pub().keyToBin())
        Assert.assertNotNull(probablePeer)
        Assert.assertEquals(address, probablePeer!!.address)
        Assert.assertEquals(lanAddress, probablePeer.lanAddress)
        Assert.assertEquals(wanAddress, probablePeer.wanAddress)
    }

    @Test
    fun addPeer_overrideAddresses() {
        val network = Network()
        val key = JavaCryptoProvider.generateKey()
        val address = Address("1.2.3.4", 1234)
        val peer = Peer(key, address)
        network.addVerifiedPeer(peer)

        val lanAddress = Address("2.3.4.5", 234)
        val wanAddress = Address("3.4.5.6", 3456)
        val newPeer = Peer(key, address, lanAddress, wanAddress)
        network.addVerifiedPeer(newPeer)

        val probablePeer = network.getVerifiedByPublicKeyBin(key.pub().keyToBin())
        Assert.assertNotNull(probablePeer)
        Assert.assertEquals(address, probablePeer!!.address)
        Assert.assertEquals(lanAddress, probablePeer.lanAddress)
        Assert.assertEquals(wanAddress, probablePeer.wanAddress)
    }
}
