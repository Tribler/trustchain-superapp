package nl.tudelft.trustchain.detoks

import nl.tudelft.ipv8.Community
import nl.tudelft.ipv8.Overlay
import nl.tudelft.ipv8.Peer
import nl.tudelft.ipv8.attestation.trustchain.BlockBuilder
import nl.tudelft.ipv8.attestation.trustchain.TrustChainBlock
import nl.tudelft.ipv8.attestation.trustchain.payload.HalfBlockBroadcastPayload
import nl.tudelft.ipv8.attestation.trustchain.payload.HalfBlockPayload
import nl.tudelft.ipv8.messaging.Packet
import nl.tudelft.ipv8.util.random
import java.util.*
import mu.KotlinLogging


open class TransactionEngine (override val serviceId: String): Community() {
    private val broadcastFanOut = 25
    private val ttl = 100
    private val logger = KotlinLogging.logger {}

    object MessageId {
        const val HALF_BLOCK: Int = 11
        const val HALF_BLOCK_ENCRYPTED: Int = 12
        const val HALF_BLOCK_BROADCAST: Int = 13
        const val HALF_BLOCK_BROADCAST_ENCRYPTED: Int = 14
    }

    init {
        messageHandlers[MessageId.HALF_BLOCK] = ::onHalfBlockPacket
        messageHandlers[MessageId.HALF_BLOCK_BROADCAST] = ::onHalfBlockBroadcastPacket
        messageHandlers[MessageId.HALF_BLOCK_ENCRYPTED] = ::onHalfBlockPacket
        messageHandlers[MessageId.HALF_BLOCK_BROADCAST_ENCRYPTED] = ::onHalfBlockBroadcastPacket
    }

    fun sendTransaction(blockBuilder: BlockBuilder, peer: Peer?, encrypt: Boolean = false) {
        logger.info { "Sending transaction..." }
        val block = blockBuilder.sign()

        if (peer != null) {
            sendBlockToRecipient(peer, block, encrypt)
        } else {
            sendBlockBroadcast(block, encrypt)
        }
    }

    private fun sendBlockToRecipient(peer: Peer, block: TrustChainBlock, encrypt: Boolean) {
        val payload = HalfBlockPayload.fromHalfBlock(block)

        val data = if (encrypt) {
            serializePacket(MessageId.HALF_BLOCK_ENCRYPTED, payload, false, encrypt = true, recipient = peer)
        } else {
            serializePacket(MessageId.HALF_BLOCK, payload, false)
        }

        send(peer, data)
    }

    private fun sendBlockBroadcast(block: TrustChainBlock, encrypt: Boolean) {
        val payload = HalfBlockBroadcastPayload.fromHalfBlock(block, ttl.toUInt())
        val randomPeers = getPeers().random(broadcastFanOut)
        for (randomPeer in randomPeers) {
            val data = if (encrypt) {
                serializePacket(MessageId.HALF_BLOCK_BROADCAST_ENCRYPTED, payload, false, encrypt = true, recipient = randomPeer)
            } else {
                serializePacket(MessageId.HALF_BLOCK_BROADCAST, payload, false)
            }
            send(randomPeer, data)
        }
    }

    private fun onHalfBlockPacket(packet: Packet) {
        logger.info { ("Half block packet received from: " + packet.source.toString()) }
    }

    private fun onHalfBlockBroadcastPacket(packet: Packet) {
        logger.info { ("Half block packet received from broadcast from: " + packet.source.toString()) }
    }

    override fun onPacket(packet: Packet) {
        val sourceAddress = packet.source
        val data = packet.data

        val probablePeer = network.getVerifiedByAddress(sourceAddress)
        if (probablePeer != null) {
            probablePeer.lastResponse = Date()
        }

        val packetPrefix = data.copyOfRange(0, prefix.size)
        if (!packetPrefix.contentEquals(prefix)) {
            // logger.debug("prefix not matching")
            return
        }

        val msgId = data[prefix.size].toUByte().toInt()

        val handler = messageHandlers[msgId]

        if (handler != null) {
            try {
                handler(packet)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        } else {
            logger.info { "Received unknown message $msgId from $sourceAddress" }
        }
    }

    class Factory(private val serviceId: String) : Overlay.Factory<TransactionEngine>(TransactionEngine::class.java) {
        override fun create(): TransactionEngine {
            return TransactionEngine(serviceId)
        }
    }
}
