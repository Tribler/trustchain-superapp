package nl.tudelft.ipv8.peerdiscovery.strategy

import io.mockk.confirmVerified
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import nl.tudelft.ipv8.Address
import nl.tudelft.ipv8.Peer
import nl.tudelft.ipv8.peerdiscovery.DiscoveryCommunity
import nl.tudelft.ipv8.peerdiscovery.Network
import org.junit.Test
import java.time.Instant
import java.util.*

class RandomChurnTest {
    @Test
    fun takeStep_active() {
        val overlay = mockk<DiscoveryCommunity>(relaxed = true)
        val network = mockk<Network>()
        val address = Address("1.2.3.4", 1234)
        val peer = Peer(mockk(), address)
        every { overlay.network } returns network
        every { network.getRandomPeers(any()) } returns listOf(peer)

        val randomChurn = RandomChurn.Factory(sampleSize = 8)
            .setOverlay(overlay).create()
        randomChurn.takeStep()

        verify { network.getRandomPeers(8) }
        confirmVerified()
    }

    @Test
    fun takeStep_inactive() {
        val overlay = mockk<DiscoveryCommunity>(relaxed = true)
        val network = mockk<Network>()
        val address = Address("1.2.3.4", 1234)
        val peer = Peer(mockk(), address)
        peer.lastResponse = Date.from(Instant.now().minusSeconds(30))
        every { overlay.network } returns network
        every { network.getRandomPeers(any()) } returns listOf(peer)

        val randomChurn = RandomChurn.Factory(sampleSize = 8)
            .setOverlay(overlay).create()
        randomChurn.takeStep()

        verify { network.getRandomPeers(8) }
        verify { overlay.sendPing(peer) }
    }

    @Test
    fun takeStep_drop() {
        val overlay = mockk<DiscoveryCommunity>(relaxed = true)
        val network = mockk<Network>(relaxed = true)
        val address = Address("1.2.3.4", 1234)
        val peer = Peer(mockk(), address)
        peer.lastResponse = Date.from(Instant.now().minusSeconds(60))
        every { overlay.network } returns network
        every { network.getRandomPeers(any()) } returns listOf(peer)

        val randomChurn = RandomChurn.Factory(sampleSize = 8)
            .setOverlay(overlay)
            .create()
        randomChurn.takeStep()

        verify { network.getRandomPeers(8) }
        verify { overlay.sendPing(peer) }

        randomChurn.takeStep()

        verify { network.removePeer(peer) }
    }
}
