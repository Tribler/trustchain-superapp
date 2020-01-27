package nl.tudelft.ipv8.attestation.trustchain

import mu.KotlinLogging
import nl.tudelft.ipv8.Address
import nl.tudelft.ipv8.Community
import nl.tudelft.ipv8.Peer
import nl.tudelft.ipv8.attestation.trustchain.payload.HalfBlockBroadcastPayload
import nl.tudelft.ipv8.attestation.trustchain.payload.HalfBlockPayload
import nl.tudelft.ipv8.attestation.trustchain.validation.TransactionValidator
import nl.tudelft.ipv8.attestation.trustchain.validation.ValidationResult
import nl.tudelft.ipv8.keyvault.CryptoProvider
import nl.tudelft.ipv8.messaging.Endpoint
import nl.tudelft.ipv8.messaging.Packet
import nl.tudelft.ipv8.messaging.payload.GlobalTimeDistributionPayload
import nl.tudelft.ipv8.peerdiscovery.Network
import nl.tudelft.ipv8.util.random
import nl.tudelft.ipv8.attestation.trustchain.ANY_COUNTERPARTY_PK
import nl.tudelft.ipv8.keyvault.PrivateKey
import nl.tudelft.ipv8.keyvault.PublicKey
import nl.tudelft.ipv8.util.contentEquals

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
    private val txValidators: MutableMap<String, TransactionValidator> = mutableMapOf()

    init {
        messageHandlers[MessageId.HALF_BLOCK] = ::onHalfBlockPacket
        messageHandlers[MessageId.HALF_BLOCK_BROADCAST] = ::onHalfBlockBroadcastPacket
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

    fun removeListener(listener: BlockListener, blockType: String? = null) {
        listenersMap[blockType]?.remove(listener)
    }

    fun registerTransactionValidator(blockType: String, validator: TransactionValidator) {
        txValidators[blockType] = validator
    }

    private fun getTransactionValidator(blockType: String): TransactionValidator? {
        return txValidators[blockType]
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
        // TODO
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
        return block
    }

    /**
     * Creates an agreement block that will be linked to the source block.
     *
     * @param source The source block which had no initial counterpary to sign.
     * @param transaction A map with supplementary information concerning the transaction.
     */
    fun createAgreementBlock(
        source: TrustChainBlock,
        transaction: TrustChainTransaction
    ): TrustChainBlock {
        val block = AgreementBlockBuilder(myPeer, database, source, transaction).sign()
        onBlockCreated(block)
        return block
    }

    private fun onBlockCreated(block: TrustChainBlock) {
        // Validate and persist
        val validation = validatePersistBlock(block)
        logger.info { "Signed block, validation result: $validation" }
        if (validation !is ValidationResult.PartialNext && validation !is ValidationResult.Valid) {
            throw RuntimeException("Signed block did not validate")
        }


    }

    /**
     * Create, sign, persist and send a block signed message.
     *
     * @param peer The peer with whom you have interacted, as a IPv8 peer.
     * @param publicKey The public key of the other party you transact with.
     * @param blockType The type of the block to be constructed, as a string.
     * @param transaction A string describing the interaction in this block.
     * @param linked The block that the requester is asking us to sign.
     * @param additionalInfo Stores additional information, on the transaction.
     */
    fun signBlock(
        peer: Peer? = null,
        publicKey: ByteArray? = null,
        blockType: String? = null,
        transaction: TrustChainTransaction? = null,
        linked: TrustChainBlock? = null,
        additionalInfo: Map<String, Any>? = null
    ): Pair<TrustChainBlock, TrustChainBlock?> {
        assert(peer != null || (linked == null &&
            publicKey?.contentEquals(ANY_COUNTERPARTY_PK) == true)) {
            "Peer, linked block should not be provided when creating a no counterparty source " +
                "block. Public key should be that reserved for any counterpary."
        }

        assert((transaction == null && linked != null) ||
            (transaction != null && linked == null)) {
            "Either provide a linked block or a transaction, not both"
        }

        assert(additionalInfo == null || (linked != null && transaction == null &&
            peer == myPeer && publicKey.contentEquals(linked.publicKey))) {
            "Either no additional info is provided or one provides it for a linked block"
        }

        assert (linked == null ||
            linked.linkPublicKey.contentEquals(myPeer.publicKey.keyToBin()) ||
            linked.linkPublicKey.contentEquals(ANY_COUNTERPARTY_PK)) {
            "Cannot counter sign block not addressed to self"
        }

        assert(linked == null || linked.linkSequenceNumber == UNKNOWN_SEQ) {
            "Cannot counter sign block that is not a request"
        }

        val builder = TrustChainBlock.Builder()

        if (linked != null) {
            builder.type = linked.type
            builder.rawTransaction = linked.rawTransaction
            builder.linkPublicKey = linked.publicKey
            builder.linkSequenceNumber = linked.sequenceNumber
        } else {
            builder.type = blockType
            builder.rawTransaction = TransactionSerialization.serialize(transaction!!)
            builder.linkPublicKey = publicKey
            builder.linkSequenceNumber = UNKNOWN_SEQ
        }

        val prevBlock = database.getLatest(myPeer.publicKey.keyToBin())
        if (prevBlock != null) {
            builder.sequenceNumber = prevBlock.sequenceNumber + 1u
            builder.previousHash = prevBlock.calculateHash()
        }

        builder.publicKey = publicKey
        builder.signature = EMPTY_SIG

        val block = builder.build()
        block.sign(myPeer.key as PrivateKey)

        // Validate and persist
        val validation = validatePersistBlock(block)
        logger.info { "Signed block, validation result: $validation" }
        if (validation !is ValidationResult.PartialNext && validation !is ValidationResult.Valid) {
            throw RuntimeException("Signed block did not validate")
        }

        // This is a source block with no counterparty
        if (peer == null && publicKey.contentEquals(ANY_COUNTERPARTY_PK)) {
            if (block.type !in settings.blockTypesBcDisabled) {
                sendBlock(block)
            }
            return Pair(block, null)
        }

        // If there is a counterparty to sign, we send it
        if (peer != null) {
            sendBlock(block, address = peer.address)
        }

        // We broadcast the block in the network if we initiated a transaction
        if (blockType !in settings.blockTypesBcDisabled && linked == null) {
            sendBlock(block)
        }

        if (peer == myPeer) {
            // We created a self-signed block
            if (block.type !in settings.blockTypesBcDisabled) {
                sendBlock(block)
            }
            // TODO: what happens here?
            return if (publicKey.contentEquals(ANY_COUNTERPARTY_PK)) {
                Pair(block, null)
            } else {
                Pair(block, linked)
            }
        } else if (linked == null) {
            // We keep track of this outstanding sign request.
            // TODO: wait for the signed block
        } else {
            // Return both half blocks.
            if (block.type !in settings.blockTypesBcDisabled) {
                sendBlockPair(linked, block)
            }
            return Pair(linked, block)
        }
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
     * We've received a half block, either because we sent a SIGNED message to some one or we are
     * crawling.
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
            sendBlock(block, ttl = payload.ttl.toInt() - 1)
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
    private fun validatePersistBlock(block: TrustChainBlock): ValidationResult {
        var validationResult = block.validate(database)

        if (validationResult !is ValidationResult.Invalid) {
            val validator = getTransactionValidator(block.type)
            if (validator != null) {
                val txValidationResult = validator.validate(block, database)
                if (txValidationResult is ValidationResult.Invalid) {
                    validationResult = txValidationResult
                }
            }

            if (validationResult !is ValidationResult.Invalid) {
                if (!database.contains(block)) {
                    database.addBlock(block)
                    notifyListeners(block)
                }
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
