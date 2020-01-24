package nl.tudelft.ipv8.attestation.trustchain.payload

import nl.tudelft.ipv8.attestation.trustchain.TrustChainBlock
import nl.tudelft.ipv8.messaging.*

/**
 * Payload for message that ships a half block.
 */
open class HalfBlockPayload(
    val publicKey: ByteArray,
    val sequenceNumber: UInt,
    val linkPublicKey: ByteArray,
    val linkSequenceNumber: UInt,
    val previousHash: ByteArray,
    val signature: ByteArray,
    val blockType: String,
    val transaction: ByteArray,
    val timestamp: ULong
) : Serializable {
    override fun serialize(): ByteArray {
        return publicKey +
            serializeUInt(sequenceNumber) +
            linkPublicKey +
            serializeUInt(linkSequenceNumber) +
            previousHash +
            signature +
            serializeVarLen(blockType.toByteArray(Charsets.US_ASCII)) +
            serializeVarLen(transaction) +
            serializeULong(timestamp)
    }

    companion object : Deserializable<HalfBlockPayload> {
        override fun deserialize(buffer: ByteArray, offset: Int): Pair<HalfBlockPayload, Int> {
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
            val payload = HalfBlockPayload(
                publicKey,
                sequenceNumber,
                linkPublicKey,
                linkSequenceNumber,
                previousHash,
                signature,
                blockType.toString(Charsets.US_ASCII),
                transaction,
                timestamp
            )
            return Pair(payload, localOffset)
        }

        fun fromHalfBlock(block: TrustChainBlock): HalfBlockPayload {
            return HalfBlockPayload(
                block.publicKey,
                block.sequenceNumber,
                block.linkPublicKey,
                block.linkSequenceNumber,
                block.previousHash,
                block.signature,
                block.type,
                block.transaction,
                block.timestamp.time.toULong()
            )
        }
    }
}
