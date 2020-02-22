package nl.tudelft.ipv8.attestation.trustchain.payload

import nl.tudelft.ipv8.attestation.trustchain.TrustChainBlock
import nl.tudelft.ipv8.messaging.*

/**
 * Payload for a broadcast message that ships two half blocks.
 */
open class HalfBlockPairBroadcastPayload(
    val block1: HalfBlockPayload,
    val block2: HalfBlockPayload,
    val ttl: UInt
) : Serializable {
    override fun serialize(): ByteArray {
        return block1.serialize() + block2.serialize() + serializeUInt(ttl)
    }

    companion object Deserializer : Deserializable<HalfBlockPairBroadcastPayload> {
        override fun deserialize(buffer: ByteArray, offset: Int): Pair<HalfBlockPairBroadcastPayload, Int> {
            val (block1, block1Size) = HalfBlockPayload.deserialize(buffer, offset)
            val (block2, block2Size) = HalfBlockPayload.deserialize(buffer, offset + block1Size)
            val ttl = deserializeUInt(buffer, offset + block1Size + block2Size)
            val payload = HalfBlockPairBroadcastPayload(
                block1, block2, ttl
            )
            return Pair(payload, block1Size + block2Size + SERIALIZED_UINT_SIZE)
        }

        fun fromHalfBlocks(block1: TrustChainBlock, block2: TrustChainBlock, ttl: UInt): HalfBlockPairBroadcastPayload {
            val payload1 = HalfBlockPayload.fromHalfBlock(block1)
            val payload2 = HalfBlockPayload.fromHalfBlock(block2)
            return HalfBlockPairBroadcastPayload(payload1, payload2, ttl)
        }
    }
}
