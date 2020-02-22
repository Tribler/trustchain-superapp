package nl.tudelft.ipv8.attestation.trustchain

import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeout
import mu.KotlinLogging
import nl.tudelft.ipv8.*
import nl.tudelft.ipv8.attestation.trustchain.validation.TransactionValidator
import nl.tudelft.ipv8.attestation.trustchain.validation.ValidationResult
import nl.tudelft.ipv8.messaging.Packet
import nl.tudelft.ipv8.util.random
import nl.tudelft.ipv8.attestation.trustchain.payload.*
import nl.tudelft.ipv8.attestation.trustchain.store.TrustChainStore
import nl.tudelft.ipv8.attestation.trustchain.validation.ValidationErrors
import nl.tudelft.ipv8.util.toHex
import java.util.*
import kotlin.coroutines.Continuation
import kotlin.math.max

private val logger = KotlinLogging.logger {}

/**
 * The community implementing TrustChain, a scalable, tamper-proof, distributed ledger. The
 * community handles sending blocks, broadcasting, and crawling chains of other peers.
 */
open class TrustChainCommunity(
    private val settings: TrustChainSettings,
    val database: TrustChainStore,
    private val crawler: TrustChainCrawler = TrustChainCrawler()
) : Community() {
    override val serviceId = "5ad767b05ae592a02488272ca2a86b847d4562e1"

    private val relayedBroadcasts = mutableSetOf<String>()
    private val listenersMap: MutableMap<String?, MutableList<BlockListener>> = mutableMapOf()
    private val txValidators: MutableMap<String, TransactionValidator> = mutableMapOf()
    private val blockSigners: MutableMap<String, BlockSigner> = mutableMapOf()

    private val crawlRequestCache: MutableMap<UInt, CrawlRequest> = mutableMapOf()

    init {
        messageHandlers[MessageId.HALF_BLOCK] = ::onHalfBlockPacket
        messageHandlers[MessageId.CRAWL_REQUEST] = ::onCrawlRequestPacket
        messageHandlers[MessageId.CRAWL_RESPONSE] = ::onCrawlResponsePacket
        messageHandlers[MessageId.HALF_BLOCK_BROADCAST] = ::onHalfBlockBroadcastPacket
        messageHandlers[MessageId.HALF_BLOCK_PAIR] = ::onHalfBlockPairPacket
        messageHandlers[MessageId.HALF_BLOCK_PAIR_BROADCAST] = ::onHalfBlockPairBroadcastPacket
        messageHandlers[MessageId.EMPTY_CRAWL_RESPONSE] = ::onEmptyCrawlResponsePacket
    }

    override fun load() {
        super.load()
        crawler.trustChainCommunity = this
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
    fun addListener(blockType: String?, listener: BlockListener) {
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
     * Register a block signer for a specific block type. The signer will be notified for every
     * incoming proposal block targeted at us. They can react by creating an agreement block.
     */
    fun registerBlockSigner(blockType: String, signer: BlockSigner) {
        blockSigners[blockType] = signer
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
     * Sends a signature request to the block signer.
     */
    private fun onSignatureRequest(block: TrustChainBlock) {
        blockSigners[block.type]?.onSignatureRequest(block)
    }

    /*
     * Request creation
     */

    /**
     * Send a block to a specific address, or do a broadcast to known peers if no peer is specified.
     */
    fun sendBlock(block: TrustChainBlock, address: Address? = null, ttl: Int = 1) {
        if (address != null) {
            logger.debug("Sending block to $address")
            val payload = HalfBlockPayload.fromHalfBlock(block)
            logger.debug("-> $payload")
            val packet = serializePacket(MessageId.HALF_BLOCK, payload, false)
            send(address, packet)
        } else {
            val payload = HalfBlockBroadcastPayload.fromHalfBlock(block, ttl.toUInt())
            logger.debug("-> $payload")
            val packet = serializePacket(MessageId.HALF_BLOCK_BROADCAST, payload, false)
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
        if (address != null) {
            val payload = HalfBlockPairPayload.fromHalfBlocks(block1, block2)
            logger.debug("-> $payload")
            val packet = serializePacket(MessageId.HALF_BLOCK_PAIR, payload, false)
            send(address, packet)
        } else {
            val payload = HalfBlockPairBroadcastPayload.fromHalfBlocks(block1, block2, ttl)
            logger.debug("-> $payload")
            val packet = serializePacket(MessageId.HALF_BLOCK_PAIR_BROADCAST, payload,
                false)
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
    suspend fun crawlChain(peer: Peer, latestBlockNum: UInt? = null) {
        crawler.crawlChain(peer, latestBlockNum)
    }

    /**
     * Send a crawl request to a specific peer.
     */
    suspend fun sendCrawlRequest(
        peer: Peer,
        publicKey: ByteArray,
        range: LongRange,
        forHalfBlock: TrustChainBlock? = null
    ): List<TrustChainBlock> = withTimeout(CRAWL_REQUEST_TIMEOUT) {
        val globalTime = claimGlobalTime()

        val crawlId = forHalfBlock?.hashNumber?.toUInt() ?: (globalTime % UInt.MAX_VALUE).toUInt()
        logger.info(
            "Requesting crawl of node $peer (blocks $range) " +
                "with id $crawlId"
        )

        val blocks = suspendCancellableCoroutine<List<TrustChainBlock>> { continuation ->
            crawlRequestCache[crawlId] = CrawlRequest(peer, continuation)

            val payload = CrawlRequestPayload(publicKey, range.first, range.last, crawlId)

            logger.debug("-> $payload")
            val packet = serializePacket(MessageId.CRAWL_REQUEST, payload)

            endpoint.send(peer.address, packet)
        }

        crawlRequestCache.remove(crawlId)

        // TODO: make sure to handle timeout properly and return at least partial crawl response

        blocks
    }

    /*
     * Request deserialization
     */

    private fun onHalfBlockPacket(packet: Packet) {
        val payload = packet.getPayload(HalfBlockPayload.Deserializer)
        onHalfBlock(packet.source, payload)
    }

    private fun onHalfBlockBroadcastPacket(packet: Packet) {
        val payload = packet.getPayload(HalfBlockBroadcastPayload.Deserializer)
        onHalfBlockBroadcast(payload)
    }

    private fun onHalfBlockPairPacket(packet: Packet) {
        val payload = packet.getPayload(HalfBlockPairPayload.Deserializer)
        onHalfBlockPair(payload)
    }

    private fun onHalfBlockPairBroadcastPacket(packet: Packet) {
        val payload = packet.getPayload(HalfBlockPairBroadcastPayload.Deserializer)
        onHalfBlockPairBroadcast(payload)
    }

    private fun onCrawlRequestPacket(packet: Packet) {
        val (peer, payload) = packet.getAuthPayload(CrawlRequestPayload.Deserializer)
        onCrawlRequest(peer, payload)
    }

    private fun onCrawlResponsePacket(packet: Packet) {
        val payload = packet.getPayload(CrawlResponsePayload.Deserializer)
        onCrawlResponse(packet.source, payload)
    }

    private fun onEmptyCrawlResponsePacket(packet: Packet) {
        val payload = packet.getPayload(EmptyCrawlResponsePayload.Deserializer)
        onEmptyCrawlResponse(payload)
    }

    /*
     * Request handling
     */

    /**
     * We've received a half block, either because we sent a signed half block to someone or we are
     * crawling.
     */
    internal fun onHalfBlock(sourceAddress: Address, payload: HalfBlockPayload) {
        logger.debug("<- $payload")

        val publicKey = cryptoProvider.keyFromPublicBin(payload.publicKey)
        val peer = Peer(publicKey, sourceAddress)

        scope.launch {
            try {
                processHalfBlock(payload.toBlock(), peer)
            } catch (e: Exception) {
                logger.error(e) { "Failed to process half block" }
            }
        }
    }

    /**
     * We received a half block, part of a broadcast. Disseminate it further.
     */
    private fun onHalfBlockBroadcast(payload: HalfBlockBroadcastPayload) {
        logger.debug("<- $payload")

        val block = payload.block.toBlock()
        validateAndPersistBlock(block)

        if (!relayedBroadcasts.contains(block.blockId) && payload.ttl > 1u) {
            sendBlock(block, ttl = payload.ttl.toInt() - 1)
        }
    }

    private fun onHalfBlockPair(payload: HalfBlockPairPayload) {
        logger.debug("<- $payload")

        validateAndPersistBlock(payload.block1.toBlock())
        validateAndPersistBlock(payload.block2.toBlock())
    }

    private fun onHalfBlockPairBroadcast(payload: HalfBlockPairBroadcastPayload) {
        logger.debug("<- $payload")

        val block1 = payload.block1.toBlock()
        val block2 = payload.block2.toBlock()
        validateAndPersistBlock(block1)
        validateAndPersistBlock(block2)

        if (block1.blockId !in relayedBroadcasts && payload.ttl > 1u) {
            sendBlockPair(block1, block2, ttl = payload.ttl - 1u)
        }
    }

    private fun onCrawlRequest(peer: Peer, payload: CrawlRequestPayload) {
        logger.debug("<- $payload")

        val startSeqNum = getPositiveSeqNum(payload.startSeqNum, payload.publicKey)
        val endSeqNum = getPositiveSeqNum(payload.endSeqNum, payload.publicKey)

        val blocks = database.crawl(payload.publicKey, startSeqNum, endSeqNum,
            limit = settings.maxCrawlBatch)

        if (blocks.isEmpty()) {
            val responsePayload = EmptyCrawlResponsePayload(payload.crawlId)
            logger.debug("-> $payload")
            val packet = serializePacket(MessageId.EMPTY_CRAWL_RESPONSE,
                responsePayload, false)
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
        val payload = CrawlResponsePayload.fromCrawl(block, crawlId,
            index.toUInt(), totalCount.toUInt())

        logger.debug("-> $payload")
        val packet = serializePacket(MessageId.CRAWL_RESPONSE, payload, false)
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

    private fun onCrawlResponse(sourceAddress: Address, payload: CrawlResponsePayload) {
        onHalfBlock(sourceAddress, payload.block)

        val block = payload.block.toBlock()

        val crawlRequest = crawlRequestCache[payload.crawlId]
        if (crawlRequest != null) {
            crawlRequest.totalHalfBlocksExpected = payload.totalCount
            crawlRequest.receivedHalfBlocks.add(block)

            if (crawlRequest.receivedHalfBlocks.size.toUInt() >=
                crawlRequest.totalHalfBlocksExpected) {
                crawlRequestCache.remove(payload.crawlId)
                crawlRequest.crawlFuture.resumeWith(
                    Result.success(crawlRequest.receivedHalfBlocks))
            }
        } else {
            logger.error { "Invalid crawl ID received: ${payload.crawlId}" }
        }
    }

    private fun onEmptyCrawlResponse(payload: EmptyCrawlResponsePayload) {
        val crawlRequest = crawlRequestCache.remove(payload.crawlId)
        if (crawlRequest != null) {
            crawlRequest.crawlFuture.resumeWith(Result.success(listOf()))
        } else {
            logger.error { "Invalid crawl ID received: ${payload.crawlId}" }
        }
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
    private suspend fun processHalfBlock(block: TrustChainBlock, peer: Peer) {
        val result = validateAndPersistBlock(block)

        logger.info("Processing half-block from $peer")

        // TODO: Check if we are waiting for this signature response

        // We can create an agreement block if this is a proposal block targeted to us and we
        // have not created a linked agreement block yet
        val canSign = block.isProposal &&
            block.linkPublicKey.contentEquals(myPeer.publicKey.keyToBin()) &&
            database.getLinked(block) == null

        if (canSign) {
            // Crawl missing blocks in case of partial validation result
            val isPartialChain = (result is ValidationResult.PartialPrevious ||
                result is ValidationResult.Partial ||
                result is ValidationResult.NoInfo)

            if (isPartialChain && settings.validationRange > 0) {
                logger.info("Proposal block could not be validated sufficiently, crawling requester")

                // TODO: check if there is crawl pending

                val range = LongRange(
                    max(
                        GENESIS_SEQ.toLong(),
                        (block.sequenceNumber.toLong() - settings.validationRange.toLong())
                    ),
                    max(GENESIS_SEQ.toLong(), block.sequenceNumber.toLong() - 1)
                )
                sendCrawlRequest(peer, block.publicKey, range, forHalfBlock = block)

                // Validate again
                processHalfBlock(block, peer)
            } else {
                onSignatureRequest(block)
            }
        }
    }

    /**
     * Validates a block and its transaction if a transaction validator has been registered for the
     * given block type. Transactions that cannot be validated are assumed to be valid.
     *
     * @return The validation result.
     */
    private fun validateBlock(block: TrustChainBlock): ValidationResult {
        var validationResult = block.validate(database)

        if (validationResult !is ValidationResult.Invalid) {
            val validator = getTransactionValidator(block.type)
            if (validator != null) {
                if (!validator.validate(block, database)) {
                    validationResult = ValidationResult.Invalid(listOf(ValidationErrors.INVALID_TRANSACTION))
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
                try {
                    logger.debug("addBlock " + block.publicKey.toHex() + " " + block.sequenceNumber)
                    database.addBlock(block)
                } catch (e: Exception) {
                    logger.error(e) { "Failed to insert block into database" }
                }
                notifyListeners(block)
            }
        } else {
            logger.warn { "Block is invalid: ${validationResult.errors}" }
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

    companion object {
        const val CRAWL_REQUEST_TIMEOUT = 10_000L
    }

    class CrawlRequest(
        val peer: Peer,
        val crawlFuture: Continuation<List<TrustChainBlock>>,
        val receivedHalfBlocks: MutableList<TrustChainBlock> = mutableListOf(),
        var totalHalfBlocksExpected: UInt = 0u
    )

    class Factory(
        private val settings: TrustChainSettings,
        private val database: TrustChainStore,
        private val crawler: TrustChainCrawler = TrustChainCrawler()
    ) : Overlay.Factory<TrustChainCommunity>(TrustChainCommunity::class.java) {
        override fun create(): TrustChainCommunity {
            return TrustChainCommunity(settings, database, crawler)
        }
    }
}
