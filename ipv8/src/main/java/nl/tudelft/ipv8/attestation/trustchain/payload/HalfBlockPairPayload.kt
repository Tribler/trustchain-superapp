package nl.tudelft.ipv8.attestation.trustchain.payload

import nl.tudelft.ipv8.attestation.trustchain.TrustChainBlock
import nl.tudelft.ipv8.messaging.*

/**
 * Payload for message that ships two half blocks.
 */
open class HalfBlockPairPayload(
    val block1: HalfBlockPayload,
    val block2: HalfBlockPayload
) : Serializable {
    override fun serialize(): ByteArray {
        return block1.serialize() + block2.serialize()
    }

    companion object Deserializer : Deserializable<HalfBlockPairPayload> {
        override fun deserialize(buffer: ByteArray, offset: Int): Pair<HalfBlockPairPayload, Int> {
            val (block1, block1Size) = HalfBlockPayload.deserialize(buffer, offset)
            val (block2, block2Size) = HalfBlockPayload.deserialize(buffer, offset + block1Size)
            val payload = HalfBlockPairPayload(
                block1, block2
            )
            return Pair(payload, block1Size + block2Size)
        }

        fun fromHalfBlocks(block1: TrustChainBlock, block2: TrustChainBlock): HalfBlockPairPayload {
            val payload1 = HalfBlockPayload.fromHalfBlock(block1)
            val payload2 = HalfBlockPayload.fromHalfBlock(block2)
            return HalfBlockPairPayload(payload1, payload2)
        }
    }
}
