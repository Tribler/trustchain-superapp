package nl.tudelft.ipv8.attestation.trustchain.payload

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
            var localOffset = 0
            val publicKey = buffer.copyOfRange(offset + localOffset, offset + localOffset + SERIALIZED_PUBLIC_KEY_SIZE)
            localOffset += SERIALIZED_PUBLIC_KEY_SIZE
            val sequenceNumber = deserializeUInt(buffer, offset + localOffset)
            localOffset += SERIALIZED_UINT_SIZE
            val linkPublicKey = buffer.copyOfRange(offset + localOffset, offset + localOffset + SERIALIZED_PUBLIC_KEY_SIZE)
            localOffset += SERIALIZED_PUBLIC_KEY_SIZE
            val linkSequenceNumber = deserializeUInt(buffer, offset + localOffset)
            localOffset += SERIALIZED_UINT_SIZE
            val previousHash = buffer.copyOfRange(offset + localOffset, offset + localOffset + HASH_SIZE)
            localOffset += HASH_SIZE
            val signature = buffer.copyOfRange(offset + localOffset, offset + localOffset + SIGNATURE_SIZE)
            localOffset += SIGNATURE_SIZE
            val (blockType, blockTypeSize) = deserializeVarLen(buffer, offset + localOffset)
            localOffset += blockTypeSize
            val (transaction, transactionSize) = deserializeVarLen(buffer, offset + localOffset)
            localOffset += transactionSize
            val timestamp = deserializeULong(buffer, offset + localOffset)
            localOffset += SERIALIZED_ULONG_SIZE
            val ttl = deserializeUInt(buffer, offset + localOffset)
            localOffset += SERIALIZED_UINT_SIZE
            val payload = HalfBlockBroadcastPayload(
                publicKey,
                sequenceNumber,
                linkPublicKey,
                linkSequenceNumber,
                previousHash,
                signature,
                blockType.toString(Charsets.US_ASCII),
                transaction,
                timestamp,
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
                block.transaction,
                block.timestamp.time.toULong(),
                ttl
            )
        }
    }
}
