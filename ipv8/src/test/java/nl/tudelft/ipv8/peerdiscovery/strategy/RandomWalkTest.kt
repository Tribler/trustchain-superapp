package nl.tudelft.ipv8.peerdiscovery.strategy

import io.mockk.*
import nl.tudelft.ipv8.Address
import nl.tudelft.ipv8.Overlay
import org.junit.Test

class RandomWalkTest {
    @Test
    fun takeStep_simple() {
        val overlay = mockk<Overlay>(relaxed = true)
        val mockAddress = Address("1.2.3.4", 1234)
        every { overlay.getWalkableAddresses() } returns listOf(mockAddress)

        val randomWalk = RandomWalk.Factory(
            resetChance = 0
        ).setOverlay(overlay).create()
        randomWalk.takeStep()

        verify { overlay.getWalkableAddresses() }
        verify { overlay.walkTo(mockAddress) }
    }

    @Test
    fun takeStep_bootstrap() {
        val overlay = mockk<Overlay>(relaxed = true)
        every { overlay.getWalkableAddresses() } returns listOf()

        val randomWalk = RandomWalk.Factory(
            resetChance = 0
        ).setOverlay(overlay).create()
        randomWalk.takeStep()

        verify { overlay.getWalkableAddresses() }
        verify { overlay.bootstrap() }
    }
}
