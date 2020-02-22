package nl.tudelft.ipv8.attestation.trustchain.payload

import nl.tudelft.ipv8.attestation.trustchain.TrustChainBlock
import nl.tudelft.ipv8.messaging.*

/**
 * Payload for a message that contains a half block and a TTL field for broadcasts.
 */
class HalfBlockBroadcastPayload(
    val block: HalfBlockPayload,
    val ttl: UInt
) : Serializable {
    override fun serialize(): ByteArray {
        return block.serialize() + serializeUInt(ttl)
    }

    companion object Deserializer : Deserializable<HalfBlockBroadcastPayload> {
        override fun deserialize(buffer: ByteArray, offset: Int): Pair<HalfBlockBroadcastPayload, Int> {
            var (block, localOffset) = HalfBlockPayload.deserialize(buffer, offset)
            val ttl = deserializeUInt(buffer, offset + localOffset)
            localOffset += SERIALIZED_UINT_SIZE
            val payload = HalfBlockBroadcastPayload(
                block,
                ttl
            )
            return Pair(payload, localOffset)
        }

        fun fromHalfBlock(block: TrustChainBlock, ttl: UInt): HalfBlockBroadcastPayload {
            return HalfBlockBroadcastPayload(
                HalfBlockPayload.fromHalfBlock(block),
                ttl
            )
        }
    }
}
