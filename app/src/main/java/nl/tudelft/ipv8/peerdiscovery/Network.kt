package nl.tudelft.ipv8.peerdiscovery

import nl.tudelft.ipv8.Address
import nl.tudelft.ipv8.Overlay
import nl.tudelft.ipv8.Peer

class Network {
    /**
     * All known addresses, mapped to (introduction peer, services)
     */
    private val allAddresses: Map<Address, Pair<Peer, Set<String>>> = mutableMapOf()

    /**
     * All verified peer objects (peer.address must be in [allAddresses])
     */
    private val verifiedPeers: List<Peer> = listOf()

    /**
     * Peers we should not add to the network (e.g. bootstrap peers)
     */
    val blacklist = mutableSetOf<Address>()

    /**
     * Excluded mids
     */
    val blacklistMids = mutableSetOf<String>()

    /**
     * A map of advertised services per peer
     */
    private val servicesPerPeer = mutableMapOf<Peer, Set<String>>()

    /**
     * A map of service identifiers to local overlays
     */
    private val serviceOverlays = mutableMapOf<String, Overlay>()

    /**
     * Cache of [Address] -> [Peer]
     */
    private val reverseIpLookup: Map<Address, Peer> = mutableMapOf()

    /**
     * Cache of [Peer] -> [[Address]]
     */
    private val reverseIntroLookup: Map<Peer, List<Address>> = mutableMapOf()

    /**
     * Cache of service ID -> [[Peer]]
     */
    private val reverseServiceLookup: Map<String, Set<Peer>> = mutableMapOf()

    private val graphLock = Object()

    //fun discoverAddress(peer: Peer, address: Address, serviceId: String? = null)
    //fun discoverServices(peer: Peer, serviceIds: List<String>)
    //fun addVerifiedPeer(peer: Peer)

    /**
     * Register an overlay to provide a certain service ID.
     *
     * @param serviceId The ID of the service.
     * @param overlay The overlay instance of the service.
     */
    fun registerServiceProvider(serviceId: String, overlay: Overlay) {
        synchronized(graphLock) {
            serviceOverlays[serviceId] = overlay
        }
    }

    fun getPeersForService(serviceId: String): List<Peer> {
        return listOf()
    }

    //fun getServicesForPeer(peer: Peer)

    /**
     * Get all addresses ready to be walked to.
     *
     * @param serviceId The service ID to filter on.
     */
    fun getWalkableAddresses(serviceId: String? = null): List<Address> {
        // TODO
        return listOf()
    }

    //fun getVerifiedByAddress(address: Address)
    //fun getVerifiedByPublicKeyBin(publicKeyBin: ByteArray)
    //fun getIntroductionFrom(peer: Peer)
    //fun removeByAddress(address: Address)
    //fun removePeer(peer: Peer)

    companion object {
        private const val REVERSE_IP_CACHE_SIZE = 500
        private const val REVERSE_INTRO_CACHE_SIZE = 500
        private const val REVERSE_SERVICE_CACHE_SIZE = 500
    }
}
