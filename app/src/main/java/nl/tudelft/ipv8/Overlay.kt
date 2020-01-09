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
    val network: Network
) : EndpointListener {
    private val globalTime: ULong
        get() = myPeer.lamportTimestamp

    /**
     * Called to inintialize this overlay.
     */
    open fun load() {
        endpoint.addListener(this)
    }

    /**
     * Called when this overlay needs to be shut down.
     */
    open fun unload() {
        endpoint.removeListener(this)
    }

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

    /**
     * Perform introduction logic to get into the network.
     */
    abstract fun bootstrap()

    /**
     * Puncture the NAT of an address.
     *
     * @param address The address to walk to.
     */
    abstract fun walkTo(address: Address)

    /**
     * Get a new IP address to walk to from a random, or selected peer.
     *
     * @param fromPeer The peer to ask for an introduction.
     */
    abstract fun getNewIntroduction(fromPeer: Peer? = null)

    /**
     * Get peers in the network that use this overlay.
     */
    abstract fun getPeers(): List<Peer>

    /**
     * Get the list of addresses we can walk to on this overlay.
     */
    abstract fun getWalkableAddresses(): List<Address>

    /**
     * Get a peer for introduction.
     *
     * @param Optionally specify a peer that is not considered eligible for introduction.
     * @return A peer to send an introduction request to, or null if are none available.
     */
    abstract fun getPeerForIntroduction(exclude: Peer? = null): Peer?
}
