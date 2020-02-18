package nl.tudelft.ipv8

import nl.tudelft.ipv8.keyvault.CryptoProvider
import nl.tudelft.ipv8.messaging.Endpoint
import nl.tudelft.ipv8.messaging.EndpointListener
import nl.tudelft.ipv8.peerdiscovery.Network

/**
 * Interface for an Internet overlay.
 */
interface Overlay : EndpointListener {
    val serviceId: String

    var myPeer: Peer
    var endpoint: Endpoint
    var network: Network
    var maxPeers: Int
    var cryptoProvider: CryptoProvider

    private val globalTime: ULong
        get() = myPeer.lamportTimestamp

    /**
     * Called to inintialize this overlay.
     */
    fun load() {
        endpoint.addListener(this)
    }

    /**
     * Called when this overlay needs to be shut down.
     */
    fun unload() {
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
    fun bootstrap()

    /**
     * Puncture the NAT of an address.
     *
     * @param address The address to walk to.
     */
    fun walkTo(address: Address)

    /**
     * Get a new IP address to walk to from a random, or selected peer.
     *
     * @param fromPeer The peer to ask for an introduction.
     */
    fun getNewIntroduction(fromPeer: Peer? = null)

    /**
     * Get peers in the network that use this overlay.
     */
    fun getPeers(): List<Peer>

    /**
     * Get the list of addresses we can walk to on this overlay.
     */
    fun getWalkableAddresses(): List<Address>

    /**
     * Get a peer for introduction.
     *
     * @param Optionally specify a peer that is not considered eligible for introduction.
     * @return A peer to send an introduction request to, or null if are none available.
     */
    fun getPeerForIntroduction(exclude: Peer? = null): Peer?

    open class Factory<T : Overlay>(
        val overlayClass: Class<T>
    ) {
        open fun create(): T {
            return overlayClass.getConstructor().newInstance()
        }
    }
}
