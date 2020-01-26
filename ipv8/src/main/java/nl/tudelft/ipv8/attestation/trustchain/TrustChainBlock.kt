package nl.tudelft.ipv8.attestation.trustchain

import nl.tudelft.ipv8.attestation.trustchain.payload.HalfBlockPayload
import nl.tudelft.ipv8.keyvault.PrivateKey
import nl.tudelft.ipv8.util.sha256
import nl.tudelft.ipv8.util.toHex
import java.util.*

val GENESIS_HASH = ByteArray(32)
val GENESIS_SEQ = 1u
val UNKNOWN_SEQ = 0u
val EMPTY_SIG = ByteArray(64)
val EMPTY_PK = ByteArray(74)

/**
 * Container for TrustChain block information.
 */
open class TrustChainBlock(
    /**
     * The block type name.
     */
    val type: String,

    /**
     * Transaction content.
     */
    val transaction: ByteArray,

    /**
     * The serialized public key of the initiator of this block.
     */
    val publicKey: ByteArray,

    /**
     * The the sequence number of this block in the chain of the intiator of this block.
     */
    val sequenceNumber: UInt,

    /**
     * The serialized public key of the counterparty of this block.
     */
    val linkPublicKey: ByteArray,

    /**
     * The height of this block in the chain of the counterparty of this block, or 0 if unknown.
     */
    val linkSequenceNumber: UInt,

    /**
     * The hash of the previous block in the chain of the initiator of this block.
     */
    val previousHash: ByteArray,

    /**
     * The signature of the initiator of this block for this block.
     */
    var signature: ByteArray,

    /**
     * The time when this block was created.
     */
    val timestamp: Date,

    /**
     * The time when this block was inserted into the local database, if this block was inserted into
     * the local database.
     */
    val insertTime: Date? = null
) {
    val blockId = publicKey.toHex() + "." + sequenceNumber

    val linkedBlockId = linkPublicKey.toHex() + "." + linkSequenceNumber

    val isGenesis = sequenceNumber == GENESIS_SEQ && previousHash.contentEquals(GENESIS_HASH)

    /**
     * Validates the transaction of this block.
     */
    open fun validateTransaction(): ValidationResult {
        return ValidationResult.VALID
    }

    /**
     * Validates this block against what is known in the database.
     */
    open fun validate(): ValidationResult {
        // TODO
        return ValidationResult.VALID
    }

    /**
     * Signs this block with the given key.
     *
     * @param key The key to sign this block with.
     */
    fun sign(key: PrivateKey) {
        val payload = HalfBlockPayload.fromHalfBlock(this).serialize()
        signature = key.sign(payload)
    }

    fun calculateHash(): ByteArray {
        val payload = HalfBlockPayload.fromHalfBlock(this).serialize()
        return sha256(payload)
    }

    companion object {
        fun fromPayload(payload: HalfBlockPayload): TrustChainBlock {
            return TrustChainBlock(
                payload.blockType,
                payload.transaction,
                payload.publicKey,
                payload.sequenceNumber,
                payload.linkPublicKey,
                payload.linkSequenceNumber,
                payload.previousHash,
                payload.signature,
                Date(payload.timestamp.toLong())
            )
        }
    }

    /**
     * Contains the various results that the validator can return.
     */
    enum class ValidationResult {
        /**
         * The block does not violate any rules.
         */
        VALID,

        /**
         * The block does not violate any rules, but there are gaps or no blocks on the previous or next block.
         */
        PARTIAL,

        /**
         * The block does not violate any rules, but there is a gap or no block on the next block.
         */
        PARTIAL_NEXT,

        /**
         * The block does not violate any rules, but there is a gap or no block on the previous block.
         */
        PARTIAL_PREVIOUS,

        /**
         * There are no blocks (previous or next) to validate against.
         */
        NO_INFO,

        /**
         * The block violates at least one validation rule.
         */
        INVALID
    }
}
