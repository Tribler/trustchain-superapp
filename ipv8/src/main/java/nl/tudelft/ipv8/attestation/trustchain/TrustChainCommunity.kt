package nl.tudelft.ipv8.attestation.trustchain

import mu.KotlinLogging
import nl.tudelft.ipv8.Address
import nl.tudelft.ipv8.Community
import nl.tudelft.ipv8.Peer
import nl.tudelft.ipv8.attestation.trustchain.validation.TransactionValidator
import nl.tudelft.ipv8.attestation.trustchain.validation.ValidationResult
import nl.tudelft.ipv8.keyvault.CryptoProvider
import nl.tudelft.ipv8.messaging.Endpoint
import nl.tudelft.ipv8.messaging.Packet
import nl.tudelft.ipv8.messaging.payload.GlobalTimeDistributionPayload
import nl.tudelft.ipv8.peerdiscovery.Network
import nl.tudelft.ipv8.util.random
import nl.tudelft.ipv8.attestation.trustchain.payload.*
import nl.tudelft.ipv8.attestation.trustchain.store.TrustChainStore
import kotlin.math.max

private val logger = KotlinLogging.logger {}

/**
 * The community implementing TrustChain, a scalable, tamper-proof, distributed ledger. The
 * community handles sending blocks, broadcasting, and crawling chains of other peers.
 */
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
    private val txValidators: MutableMap<String, TransactionValidator> = mutableMapOf()

    init {
        messageHandlers[MessageId.HALF_BLOCK] = ::onHalfBlockPacket
        messageHandlers[MessageId.CRAWL_REQUEST] = ::onCrawlRequestPacket
        messageHandlers[MessageId.CRAWL_RESPONSE] = ::onCrawlResponsePacket
        messageHandlers[MessageId.HALF_BLOCK_BROADCAST] = ::onHalfBlockBroadcastPacket
        messageHandlers[MessageId.HALF_BLOCK_PAIR] = ::onHalfBlockPairPacket
        messageHandlers[MessageId.HALF_BLOCK_PAIR_BROADCAST] = ::onHalfBlockPairBroadcastPacket
        messageHandlers[MessageId.EMPTY_CRAWL_RESPONSE] = ::onEmptyCrawlResponsePacket
    }

    /*
     * Block listeners
     */

    /**
     * Registers a listener that will be notified of new blocks.
     *
     * @param listener The listener to be notified.
     * @param blockType The type of blocks the listener will be notified about.
     * If null, the listener will be notified about all block types.
     */
    fun addListener(listener: BlockListener, blockType: String? = null) {
        val listeners = listenersMap[blockType] ?: mutableListOf()
        listenersMap[blockType] = listeners
        listeners.add(listener)
    }

    /**
     * Removes a previously registered block listener.
     *
     * @param listener The listener to be removed.
     * @param blockType The block type to which the listener has been registered.
     */
    fun removeListener(listener: BlockListener, blockType: String? = null) {
        listenersMap[blockType]?.remove(listener)
    }

    /**
     * Registers a validator for a specific block type. The validator is called for every incoming
     * block. It should check the integrity of the transaction and return the validation result.
     * Invalid blocks will be dropped immediately, valid blocks will be stored in the database.
     *
     * @param blockType The block type the validator is able to validate.
     * @param validator The transaction validator.
     */
    fun registerTransactionValidator(blockType: String, validator: TransactionValidator) {
        txValidators[blockType] = validator
    }

    private fun getTransactionValidator(blockType: String): TransactionValidator? {
        return txValidators[blockType]
    }

    /**
     * Notify listeners of a specific new block.
     */
    internal fun notifyListeners(block: TrustChainBlock) {
        val universalListeners = listenersMap[null] ?: listOf<BlockListener>()
        for (listener in universalListeners) {
            listener.onBlockReceived(block)
        }

        val listeners = listenersMap[block.type] ?: listOf<BlockListener>()
        for (listener in listeners) {
            listener.onBlockReceived(block)
        }
    }

    /**
     * Return whether we should sign the block in the passed message.
     */
    private fun shouldSign(block: TrustChainBlock): Boolean {
        val listeners: List<BlockListener> = listenersMap[block.type] ?: listOf()
        for (listener in listeners) {
            if (listener.shouldSign(block)) {
                return true
            }
        }
        return false
    }

    /*
     * Request creation
     */

    /**
     * Send a block to a specific address, or do a broadcast to known peers if no peer is specified.
     */
    fun sendBlock(block: TrustChainBlock, address: Address? = null, ttl: Int = 1) {
        val globalTime = claimGlobalTime()
        val dist = GlobalTimeDistributionPayload(globalTime)

        if (address != null) {
            logger.debug("Sending block to $address")
            val payload = HalfBlockPayload.fromHalfBlock(block)
            val packet = serializePacket(prefix, MessageId.HALF_BLOCK, listOf(dist, payload),
                false)
            send(address, packet)
        } else {
            val payload = HalfBlockBroadcastPayload.fromHalfBlock(block, ttl.toUInt())
            val packet = serializePacket(prefix, MessageId.HALF_BLOCK_BROADCAST, listOf(dist,
                payload), false)
            val randomPeers = getPeers().random(settings.broadcastFanout)
            for (peer in randomPeers) {
                send(peer.address, packet)
            }
            relayedBroadcasts.add(block.blockId)
        }
    }

    /**
     * Send a half block pair to a specific address, or do a broadcast to known peers if no peer
     * is specified.
     */
    fun sendBlockPair(
        block1: TrustChainBlock,
        block2: TrustChainBlock,
        address: Address? = null,
        ttl: UInt = 1u
    ) {
        val globalTime = claimGlobalTime()
        val dist = GlobalTimeDistributionPayload(globalTime)

        if (address != null) {
            val payload = HalfBlockPairPayload.fromHalfBlocks(block1, block2)
            val packet = serializePacket(prefix, MessageId.HALF_BLOCK_PAIR, listOf(dist, payload),
                false)
            send(address, packet)
        } else {
            val payload = HalfBlockPairBroadcastPayload.fromHalfBlocks(block1, block2, ttl)
            val packet = serializePacket(prefix, MessageId.HALF_BLOCK_PAIR_BROADCAST,
                listOf(dist, payload))
            for (peer in network.getRandomPeers(settings.broadcastFanout)) {
                send(peer.address, packet)
            }
            relayedBroadcasts.add(block1.blockId)
        }
    }

    /**
     * Creates a proposal block.
     *
     * @param blockType The type of the block to be constructed, as a string.
     * @param transaction A map describing the interaction in this block.
     * @param publicKey The bin encoded public key of the other party you transact with. Set to
     * [ANY_COUNTERPARTY_PK] constant to create a source block without any initial counterparty to
     * sign. Set to your own public key to create a self-signed block.
     */
    fun createProposalBlock(
        blockType: String,
        transaction: TrustChainTransaction,
        publicKey: ByteArray
    ): TrustChainBlock {
        val block = ProposalBlockBuilder(myPeer, database, blockType, transaction, publicKey).sign()

        onBlockCreated(block)

        // Broadcast the block in the network if we initiated a transaction
        if (block.type !in settings.blockTypesBcDisabled) {
            sendBlock(block)
        }

        // We keep track of this outstanding sign request
        if (!publicKey.contentEquals(myPeer.publicKey.keyToBin())) {
            // TODO: implement HalfBlockSignCache
        }

        return block
    }

    /**
     * Creates an agreement block that will be linked to the proposal block.
     *
     * @param link The proposal block which the agreement block will be linked to.
     * @param transaction A map with supplementary information concerning the transaction.
     */
    fun createAgreementBlock(
        link: TrustChainBlock,
        transaction: TrustChainTransaction
    ): TrustChainBlock {
        assert(link.linkPublicKey.contentEquals(myPeer.publicKey.keyToBin()) ||
            link.linkPublicKey.contentEquals(ANY_COUNTERPARTY_PK)) {
            "Cannot counter sign block not addressed to self"
        }

        assert(link.linkSequenceNumber == UNKNOWN_SEQ) {
            "Cannot counter sign block that is not a request"
        }

        val block = AgreementBlockBuilder(myPeer, database, link, transaction).sign()

        onBlockCreated(block)

        // Broadcast both half blocks
        if (block.type !in settings.blockTypesBcDisabled) {
            sendBlockPair(link, block)
        }

        return block
    }

    private fun onBlockCreated(block: TrustChainBlock) {
        // Validate and persist
        val validation = validateAndPersistBlock(block)

        logger.info { "Signed block, validation result: $validation" }

        if (validation !is ValidationResult.PartialNext && validation !is ValidationResult.Valid) {
            throw RuntimeException("Signed block did not validate")
        }

        val peer = network.getVerifiedByPublicKeyBin(block.linkPublicKey)
        if (peer != null) {
            // If there is a counterparty to sign, we send it
            sendBlock(block, address = peer.address)
        }
    }

    /**
     * Crawl the whole chain of a specific peer.
     *
     * @param latestBlockNum The latest block number of the peer in question, if available.
     */
    fun crawlChain(peer: Peer, latestBlockNum: Int = 0) {
        // TODO: check if crawl is already pending
    }

    private fun sendNextPartialChainCrawlRequest() {
    }

    /*
     * Request deserialization
     */

    internal fun onHalfBlockPacket(packet: Packet) {
        val payload = packet.getPayload(HalfBlockPayload.Companion)
        onHalfBlock(packet.source, payload)
    }

    internal fun onHalfBlockBroadcastPacket(packet: Packet) {
        val payload = packet.getPayload(HalfBlockBroadcastPayload.Companion)
        onHalfBlockBroadcast(payload)
    }

    internal fun onHalfBlockPairPacket(packet: Packet) {
        val payload = packet.getPayload(HalfBlockPairPayload.Companion)
        onHalfBlockPair(payload)
    }

    internal fun onHalfBlockPairBroadcastPacket(packet: Packet) {
        val payload = packet.getPayload(HalfBlockPairBroadcastPayload.Companion)
        onHalfBlockPairBroadcast(payload)
    }

    internal fun onCrawlRequestPacket(packet: Packet) {
        val (peer, payload) = packet.getAuthPayload(CrawlRequestPayload.Companion, cryptoProvider)
        onCrawlRequest(peer, payload)
    }

    internal fun onCrawlResponsePacket(packet: Packet) {
        val payload = packet.getPayload(CrawlResponsePayload.Companion)
        onCrawlResponse(packet.source, payload)
    }

    internal fun onEmptyCrawlResponsePacket(packet: Packet) {
        val payload = packet.getPayload(EmptyCrawlResponsePayload.Companion)
        onEmptyCrawlResponse(payload)
    }

    /*
     * Request handling
     */

    /**
     * We've received a half block, either because we sent a SIGNED message to some one or we are
     * crawling.
     */
    fun onHalfBlock(sourceAddress: Address, payload: HalfBlockPayload) {
        val publicKey = cryptoProvider.keyFromPublicBin(payload.publicKey)
        val peer = Peer(publicKey, sourceAddress)

        try {
            processHalfBlock(payload.toBlock(), peer)
        } catch (e: Exception) {
            logger.error(e) { "Failed to process half block" }
        }
    }

    /**
     * We received a half block, part of a broadcast. Disseminate it further.
     */
    fun onHalfBlockBroadcast(payload: HalfBlockBroadcastPayload) {
        val block = payload.block.toBlock()
        validateAndPersistBlock(block)

        if (!relayedBroadcasts.contains(block.blockId) && payload.ttl > 1u) {
            sendBlock(block, ttl = payload.ttl.toInt() - 1)
        }
    }

    fun onHalfBlockPair(payload: HalfBlockPairPayload) {
        validateAndPersistBlock(payload.block1.toBlock())
        validateAndPersistBlock(payload.block2.toBlock())
    }

    fun onHalfBlockPairBroadcast(payload: HalfBlockPairBroadcastPayload) {
        val block1 = payload.block1.toBlock()
        val block2 = payload.block2.toBlock()
        validateAndPersistBlock(block1)
        validateAndPersistBlock(block2)

        if (block1.blockId !in relayedBroadcasts && payload.ttl > 1u) {
            sendBlockPair(block1, block2, ttl = payload.ttl - 1u)
        }
    }

    fun onCrawlRequest(peer: Peer, payload: CrawlRequestPayload) {
        val startSeqNum = getPositiveSeqNum(payload.startSeqNum, payload.publicKey)
        val endSeqNum = getPositiveSeqNum(payload.endSeqNum, payload.publicKey)

        val blocks = database.crawl(payload.publicKey, startSeqNum, endSeqNum,
            limit = settings.maxCrawlBatch)

        if (blocks.isEmpty()) {
            val globalTime = claimGlobalTime()
            val responsePayload = EmptyCrawlResponsePayload(payload.crawlId)
            val dist = GlobalTimeDistributionPayload(globalTime)
            val packet = serializePacket(prefix, MessageId.EMPTY_CRAWL_RESPONSE,
                listOf(dist, responsePayload), false)
            send(peer.address, packet)
        } else {
            sendCrawlResponses(blocks, peer, payload.crawlId)
        }
    }

    private fun sendCrawlResponses(blocks: List<TrustChainBlock>, peer: Peer, crawlId: UInt) {
        for ((i, block) in blocks.withIndex()) {
            sendCrawlResponse(block, crawlId, i + 1, blocks.size, peer)
        }
        logger.info { "Sent ${blocks.size} blocks" }
    }

    private fun sendCrawlResponse(
        block: TrustChainBlock,
        crawlId: UInt,
        index: Int,
        totalCount: Int,
        peer: Peer
    ) {

        val globalTime = claimGlobalTime()
        val payload = CrawlResponsePayload.fromCrawl(block, crawlId, index.toUInt(),
            totalCount.toUInt())
        val dist = GlobalTimeDistributionPayload(globalTime)

        val packet = serializePacket(prefix, MessageId.CRAWL_RESPONSE, listOf(dist, payload), false)
        send(peer.address, packet)
    }

    /**
     * Converts a negative sequence number to a positive number, based on the last block of one's
     * chain.
     */
    private fun getPositiveSeqNum(seqNum: Long, publicKey: ByteArray): Long {
        return if (seqNum < 0) {
            val lastBlock = database.getLatest(publicKey)
            return if (lastBlock != null) {
                max(GENESIS_SEQ.toLong(), lastBlock.sequenceNumber.toLong() + seqNum + 1)
            } else {
                GENESIS_SEQ.toLong()
            }
        } else {
            seqNum
        }
    }

    fun onCrawlResponse(sourceAddress: Address, payload: CrawlResponsePayload) {
        onHalfBlock(sourceAddress, payload.block)

        val block = payload.block.toBlock()

        // TODO: remove crawl id from request cache
    }

    fun onEmptyCrawlResponse(payload: EmptyCrawlResponsePayload) {
        // TODO: remove crawl id from request cache
    }

    /**
     * Return the length of your own chain.
     */
    fun getChainLength(): Int {
        val latestBlock = database.getLatest(myPeer.publicKey.keyToBin())
        return latestBlock?.sequenceNumber?.toInt() ?: 0
    }

    /**
     * Process a received half block.
     */
    private fun processHalfBlock(block: TrustChainBlock, peer: Peer) {
        validateAndPersistBlock(block)
    }

    /**
     * Validates a block and its transaction if a transaction validator has been registered for the
     * given block type.
     *
     * @return The validation result.
     */
    internal fun validateBlock(block: TrustChainBlock): ValidationResult {
        var validationResult = block.validate(database)

        if (validationResult !is ValidationResult.Invalid) {
            val validator = getTransactionValidator(block.type)
            if (validator != null) {
                val txValidationResult = validator.validate(block, database)
                if (txValidationResult is ValidationResult.Invalid) {
                    validationResult = txValidationResult
                }
            }
        }

        return validationResult
    }

    /**
     * Validates a block and if it's valid, persists it.
     *
     * @return The validation result.
     */
    internal fun validateAndPersistBlock(block: TrustChainBlock): ValidationResult {
        val validationResult = validateBlock(block)

        if (validationResult !is ValidationResult.Invalid) {
            if (!database.contains(block)) {
                database.addBlock(block)
                notifyListeners(block)
            }
        }

        return validationResult
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
