package nl.tudelft.ipv8

import nl.tudelft.ipv8.messaging.Endpoint
import nl.tudelft.ipv8.messaging.EndpointListener
import nl.tudelft.ipv8.peerdiscovery.Network

/**
 * Interface for an Internet overlay.
 */
abstract class Overlay(
    protected val myPeer: Peer,
    protected val endpoint: Endpoint,
    protected val network: Network
) : EndpointListener {
    private val globalTime: ULong
        get() = myPeer.lamportTimestamp

    /**
     * Increments the current global time by one and returns this value.
     */
    fun claimGlobalTime(): ULong {
        updateGlobalTime(globalTime + 1u)
        return globalTime
    }

    /**
     * Increase the local global time if the given [globalTime] is larger.
     */
    private fun updateGlobalTime(globalTime: ULong) {
        if (globalTime > this.globalTime) {
            myPeer.updateClock(globalTime)
        }
    }
}
