package nl.tudelft.ipv8.attestation.trustchain.payload

import nl.tudelft.ipv8.attestation.trustchain.TransactionSerialization
import nl.tudelft.ipv8.attestation.trustchain.TrustChainBlock
import nl.tudelft.ipv8.messaging.*

/**
 * Payload for a message that contains a half block and a TTL field for broadcasts.
 */
class HalfBlockBroadcastPayload(
    publicKey: ByteArray,
    sequenceNumber: UInt,
    linkPublicKey: ByteArray,
    linkSequenceNumber: UInt,
    previousHash: ByteArray,
    signature: ByteArray,
    blockType: String,
    transaction: ByteArray,
    timestamp: ULong,
    val ttl: UInt
) : HalfBlockPayload(
    publicKey,
    sequenceNumber,
    linkPublicKey,
    linkSequenceNumber,
    previousHash,
    signature,
    blockType,
    transaction,
    timestamp
) {
    override fun serialize(): ByteArray {
        return super.serialize() + serializeUInt(ttl)
    }

    companion object : Deserializable<HalfBlockBroadcastPayload> {
        override fun deserialize(buffer: ByteArray, offset: Int): Pair<HalfBlockBroadcastPayload, Int> {
            var (block, localOffset) = HalfBlockPayload.deserialize(buffer, offset)
            val ttl = deserializeUInt(buffer, offset + localOffset)
            localOffset += SERIALIZED_UINT_SIZE
            val payload = HalfBlockBroadcastPayload(
                block.publicKey,
                block.sequenceNumber,
                block.linkPublicKey,
                block.linkSequenceNumber,
                block.previousHash,
                block.signature,
                block.blockType,
                block.transaction,
                block.timestamp,
                ttl
            )
            return Pair(payload, localOffset)
        }

        fun fromHalfBlock(block: TrustChainBlock, ttl: UInt): HalfBlockBroadcastPayload {
            return HalfBlockBroadcastPayload(
                block.publicKey,
                block.sequenceNumber,
                block.linkPublicKey,
                block.linkSequenceNumber,
                block.previousHash,
                block.signature,
                block.type,
                TransactionSerialization.serialize(block.transaction),
                block.timestamp.time.toULong(),
                ttl
            )
        }
    }
}
