package nl.tudelft.ipv8.attestation.trustchain

import mu.KotlinLogging
import nl.tudelft.ipv8.Address
import nl.tudelft.ipv8.Community
import nl.tudelft.ipv8.Peer
import nl.tudelft.ipv8.attestation.trustchain.payload.HalfBlockBroadcastPayload
import nl.tudelft.ipv8.attestation.trustchain.payload.HalfBlockPayload
import nl.tudelft.ipv8.keyvault.CryptoProvider
import nl.tudelft.ipv8.messaging.Endpoint
import nl.tudelft.ipv8.messaging.Packet
import nl.tudelft.ipv8.messaging.payload.GlobalTimeDistributionPayload
import nl.tudelft.ipv8.peerdiscovery.Network
import nl.tudelft.ipv8.util.random

private val logger = KotlinLogging.logger {}

class TrustChainCommunity(
    myPeer: Peer,
    endpoint: Endpoint,
    network: Network,
    val settings: TrustChainSettings,
    val database: TrustChainStore,
    maxPeers: Int,
    cryptoProvider: CryptoProvider
) : Community(myPeer, endpoint, network, maxPeers, cryptoProvider) {
    override val serviceId = "5ad767b05ae592a02488272ca2a86b847d4562e1"

    private val relayedBroadcasts = mutableSetOf<String>()
    private val listenersMap: MutableMap<String?, MutableList<BlockListener>> = mutableMapOf()

    init {
        messageHandlers[MessageId.HALF_BLOCK] = ::onHalfBlockPacket
        messageHandlers[MessageId.HALF_BLOCK_BROADCAST] = ::onHalfBlockBroadcastPacket
    }

    /*
     * Block listeners
     */

    fun addListener(listener: BlockListener, blockType: String? = null) {
        val listeners = listenersMap[blockType] ?: mutableListOf()
        listenersMap[blockType] = listeners
        listeners.add(listener)
    }

    fun removeListener(listener: BlockListener, blockType: String? = null) {
        listenersMap[blockType]?.remove(listener)
    }

    /**
     * Notify listeners of a specific new block.
     */
    private fun notifyListeners(block: TrustChainBlock) {
        val universalListeners = listenersMap[null] ?: listOf<BlockListener>()
        for (listener in universalListeners) {
            listener.onBlockReceived(block)
        }

        val listeners = listenersMap[block.type] ?: listOf<BlockListener>()
        for (listener in listeners) {
            listener.onBlockReceived(block)
        }
    }

    /*
     * Request creation
     */

    /**
     * Send a block to a specific address, or do a broadcast to known peers if no peer is specified.
     */
    fun sendBlock(block: TrustChainBlock, address: Address? = null, ttl: UInt = 1u) {
        val globalTime = claimGlobalTime()
        val dist = GlobalTimeDistributionPayload(globalTime)

        if (address != null) {
            logger.debug("Sending block to $address")
            val payload = HalfBlockPayload.fromHalfBlock(block)
            val packet = serializePacket(prefix, MessageId.HALF_BLOCK, listOf(dist, payload), false)
            send(address, packet)
        } else {
            val payload = HalfBlockBroadcastPayload.fromHalfBlock(block, ttl)
            val packet = serializePacket(prefix, MessageId.HALF_BLOCK_BROADCAST, listOf(dist, payload), false)
            val randomPeers = getPeers().random(settings.broadcastFanout)
            for (peer in randomPeers) {
                send(peer.address, packet)
            }
            relayedBroadcasts.add(block.blockId)
        }
    }

    /**
     * Send a half block pair to a specific address, or do a broadcast to known peers if no peer is specified.
     */
    fun sendBlockPair(block1: TrustChainBlock, block2: TrustChainBlock, address: Address? = null, ttl: UInt = 1u) {
        // TODO
    }

    /**
     * Create a source block without any initial counterparty to sign.
     */
    fun createSourceBlock() {
        // TODO
    }

    /**
     * Create a Link Block to a source block.
     */
    fun createLinkBlock() {
        // TODO
    }

    /**
     * Create, sign, persist and send a block signed message.
     */
    fun signBlock(peer: Peer) {
        // TODO
    }

    /**
     * Crawl the whole chain of a specific peer.
     */
    fun crawlChain(peer: Peer, latestBlockNum: Int = 0) {

    }

    /*
     * Request deserialization
     */

    internal fun onHalfBlockPacket(packet: Packet) {
        val remainder = packet.getPayload()
        val (_, distSize) = GlobalTimeDistributionPayload.deserialize(remainder)
        val (payload, _) = HalfBlockPayload.deserialize(remainder, distSize)
        onHalfBlock(packet.source, payload)
    }

    internal fun onHalfBlockBroadcastPacket(packet: Packet) {
        val remainder = packet.getPayload()
        val (_, distSize) = GlobalTimeDistributionPayload.deserialize(remainder)
        val (payload, _) = HalfBlockBroadcastPayload.deserialize(remainder, distSize)
        onHalfBlockBroadcast(payload)
    }

    /*
     * Request handling
     */

    /**
     * We've received a half block, either because we sent a SIGNED message to some one or we are crawling.
     */
    fun onHalfBlock(sourceAddress: Address, payload: HalfBlockPayload) {
        val publicKey = cryptoProvider.keyFromPublicBin(payload.publicKey)
        val peer = Peer(publicKey, sourceAddress)
        val block = TrustChainBlock.fromPayload(payload)
        try {
            processHalfBlock(block, peer)
        } catch (e: Exception) {
            logger.error(e) { "Failed to process half block" }
        }
    }

    /**
     * We received a half block, part of a broadcast. Disseminate it further.
     */
    fun onHalfBlockBroadcast(payload: HalfBlockBroadcastPayload) {
        val block = TrustChainBlock.fromPayload(payload)
        validatePersistBlock(block)

        if (!relayedBroadcasts.contains(block.blockId) && payload.ttl > 0u) {
            sendBlock(block, ttl = payload.ttl - 1u)
        }
    }

    /**
     * Process a received half block.
     */
    private fun processHalfBlock(block: TrustChainBlock, peer: Peer) {
        validatePersistBlock(block)
    }

    /**
     * Validate a block and if it's valid, persist it. Return the validation result.
     */
    private fun validatePersistBlock(block: TrustChainBlock) {
        val validation = block.validate()
        if (validation != TrustChainBlock.ValidationResult.INVALID) {
            if (!database.contains(block)) {
                database.addBlock(block)
                notifyListeners(block)
            }
        }
    }

    object MessageId {
        const val HALF_BLOCK = 1
        const val CRAWL_REQUEST = 2
        const val CRAWL_RESPONSE = 3
        const val HALF_BLOCK_PAIR = 4
        const val HALF_BLOCK_BROADCAST = 5
        const val HALF_BLOCK_PAIR_BROADCAST = 6
        const val EMPTY_CRAWL_RESPONSE = 7
    }
}
