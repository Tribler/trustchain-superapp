package nl.tudelft.ipv8.peerdiscovery

import nl.tudelft.ipv8.Address
import nl.tudelft.ipv8.Overlay
import nl.tudelft.ipv8.Peer
import kotlin.math.min

class Network {
    /**
     * All known addresses, mapped to (introduction peer MID, service ID)
     */
    val allAddresses: MutableMap<Address, Pair<String, String?>> = mutableMapOf()

    /**
     * All verified peer objects (peer.address must be in [allAddresses])
     */
    val verifiedPeers: MutableSet<Peer> = mutableSetOf()

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
    private val servicesPerPeer = mutableMapOf<String, MutableSet<String>>()

    /**
     * A map of service identifiers to local overlays
     */
    val serviceOverlays = mutableMapOf<String, Overlay>()

    val graphLock = Object()

    /**
     * A peer has introduced us to another IP address.
     *
     * @param peer The peer that performed the introduction.
     * @param address The introduced address.
     * @param serviceId The service through which we discovered the peer.
     */
    fun discoverAddress(peer: Peer, address: Address, serviceId: String? = null) {
        if (address in blacklist) {
            addVerifiedPeer(peer)
            return
        }

        synchronized(graphLock) {
            if (address !in allAddresses || allAddresses[address]!!.first !in verifiedPeers.map { it.mid }) {
                // This is a new address, or our previous parent has been removed
                allAddresses[address] = Pair(peer.mid, serviceId)
            }
            addVerifiedPeer(peer)
        }
    }

    /**
     * A peer has advertised some services he can use.
     *
     * @param peer The peer to update the services for.
     * @param serviceIds The list of service IDs to register.
     */
    fun discoverServices(peer: Peer, serviceIds: List<String>) {
        synchronized(graphLock) {
            val peerServices = servicesPerPeer[peer.mid] ?: mutableSetOf()
            peerServices.addAll(serviceIds)
            servicesPerPeer[peer.mid] = peerServices
        }
    }

    /**
     * The holepunching layer has a new peer for us.
     *
     * @param peer The new peer.
     */
    fun addVerifiedPeer(peer: Peer) {
        if (peer.mid in blacklistMids) return

        synchronized(graphLock) {
            // This may just be an address update
            for (known in verifiedPeers) {
                if (known.mid == peer.mid) {
                    known.address = peer.address
                    if (!peer.lanAddress.isEmpty()) {
                        known.lanAddress = peer.lanAddress
                    }
                    if (!peer.wanAddress.isEmpty()) {
                        known.wanAddress = peer.wanAddress
                    }
                    return
                }
            }

            if (peer.address in allAddresses) {
                if (peer !in verifiedPeers) {
                    verifiedPeers.add(peer)
                }
            } else if (peer.address !in blacklist) {
                if (peer.address !in allAddresses) {
                    allAddresses[peer.address] = Pair("", null)
                }
                if (peer !in verifiedPeers) {
                    verifiedPeers.add(peer)
                }
            }
        }
    }

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

    /**
     * Get peers which support a certain service.
     *
     * @param serviceId The service ID to fetch peers for.
     */
    fun getPeersForService(serviceId: String): List<Peer> {
        val out = mutableListOf<Peer>()
        synchronized(graphLock) {
            for (peer in verifiedPeers) {
                val peerServices = servicesPerPeer[peer.mid]
                if (peerServices != null) {
                    if (serviceId in peerServices) {
                        out += peer
                    }
                }
            }
        }
        return out
    }

    /**
     * Get the known services supported by a peer.
     *
     * @param peer The peer to check services for.
     */
    fun getServicesForPeer(peer: Peer): Set<String> {
        synchronized(graphLock) {
            return servicesPerPeer[peer.mid] ?: setOf()
        }
    }

    /**
     * Get all addresses ready to be walked to.
     *
     * @param serviceId The service ID to filter on.
     */
    fun getWalkableAddresses(serviceId: String? = null): List<Address> {
        synchronized(graphLock) {
            val known = if (serviceId != null) getPeersForService(serviceId) else verifiedPeers
            val knownAddresses = known.map { it.address }
            var out = (allAddresses.keys.toSet() - knownAddresses).toList()

            if (serviceId != null) {
                val newOut = mutableListOf<Address>()

                for (address in out) {
                    val (introPeer, service) = allAddresses[address] ?: return listOf()
                    val services = servicesPerPeer[introPeer] ?: mutableSetOf()
                    if (service != null) {
                        services += service
                    }

                    // If the one that introduced this peer runs the requested service, there is
                    // a chance the introduced peer will run it too.
                    if (serviceId in services) {
                        newOut += address
                    }
                }

                out = newOut
            }

            return out
        }
    }

    /**
     * Get a verified peer by its IP address. If multiple peers use the same IP address,
     * this method returns only one of those peers.
     *
     * @param address The address to search for.
     * @return The [Peer] object for this address or null.
     */
    fun getVerifiedByAddress(address: Address): Peer? {
        synchronized(graphLock) {
            return verifiedPeers.find { it.address == address }
        }
    }

    /**
     * Get a verified peer by its public key bin.
     *
     * @param publicKeyBin The string representation of the public key.
     * @return The [Peer] object for this public key or null.
     */
    fun getVerifiedByPublicKeyBin(publicKeyBin: ByteArray): Peer? {
        synchronized(graphLock) {
            return verifiedPeers.find { it.publicKey.keyToBin().contentEquals(publicKeyBin) }
        }
    }

    /**
     * Get the addresses introduced to us by a certain peer.
     *
     * @param peer The peer to get the introductions for.
     * @return A list of the introduced addresses.
     */
    fun getIntroductionFrom(peer: Peer): List<Address> {
        synchronized(graphLock) {
            return allAddresses
                .filter { it.value.first == peer.mid }
                .map { it.key }
        }
    }

    /**
     * Remove all walkable addresses and verified peers using a certain IP address.
     *
     * @param address The address to remove.
     */
    fun removeByAddress(address: Address) {
        synchronized(graphLock) {
            allAddresses.remove(address)
            verifiedPeers.removeAll { it.address == address }
            // TODO: what about servicesPerPeer?
        }
    }

    /**
     * Remove a verified peer.
     *
     * @param peer The peer to remove.
     */
    fun removePeer(peer: Peer) {
        synchronized(graphLock) {
            allAddresses.remove(peer.address)
            verifiedPeers.remove(peer)
            servicesPerPeer.remove(peer.mid)
        }
    }

    /**
     * Returns a random verified peer.
     */
    fun getRandomPeer(): Peer? {
        synchronized(graphLock) {
            return if (verifiedPeers.isNotEmpty()) verifiedPeers.random() else null
        }
    }

    /**
     * Returns a random sample of verified peers of the maximum size of [maxSampleSize].
     */
    fun getRandomPeers(maxSampleSize: Int): List<Peer> {
        synchronized(graphLock) {
            val sampleSize = min(verifiedPeers.size, maxSampleSize)
            val shuffled = verifiedPeers.shuffled()
            return shuffled.subList(0, sampleSize)
        }
    }
}
