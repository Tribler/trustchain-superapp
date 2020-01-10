package nl.tudelft.ipv8

import io.mockk.spyk
import nl.tudelft.ipv8.keyvault.Key
import org.junit.Assert
import org.junit.Test

class PeerTest {
    @Test
    fun updateClock() {
        val key = spyk<Key>()
        val peer = Peer(key)
        Assert.assertEquals(0uL, peer.lamportTimestamp)
        peer.updateClock(1000uL)
        Assert.assertEquals(1000uL, peer.lamportTimestamp)
        peer.updateClock(1uL)
        Assert.assertEquals(1000uL, peer.lamportTimestamp)
    }
}
