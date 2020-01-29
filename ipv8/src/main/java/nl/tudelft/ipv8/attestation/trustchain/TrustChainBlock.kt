package nl.tudelft.ipv8.attestation.trustchain

import nl.tudelft.ipv8.attestation.trustchain.payload.HalfBlockPayload
import nl.tudelft.ipv8.attestation.trustchain.store.TrustChainStore
import nl.tudelft.ipv8.attestation.trustchain.validation.ValidationResult
import nl.tudelft.ipv8.keyvault.PrivateKey
import nl.tudelft.ipv8.util.sha256
import nl.tudelft.ipv8.util.toHex
import java.util.*

val GENESIS_HASH = ByteArray(32)
val GENESIS_SEQ = 1u
val UNKNOWN_SEQ = 0u
val EMPTY_SIG = ByteArray(64)
val EMPTY_PK = ByteArray(74)
val ANY_COUNTERPARTY_PK = EMPTY_PK

typealias TrustChainTransaction = Map<*, *>

/**
 * Container for TrustChain block information.
 */
class TrustChainBlock(
    /**
     * The block type name.
     */
    val type: String,

    /**
     * Transaction content.
     */
    val rawTransaction: ByteArray,

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

    val transaction: TrustChainTransaction by lazy {
        TransactionSerialization.deserialize(rawTransaction)
    }

    /**
     * Validates this block against what is known in the database.
     */
    fun validate(database: TrustChainStore): ValidationResult {
        // TODO
        return ValidationResult.Valid()
    }

    /**
     * Signs this block with the given key.
     *
     * @param key The key to sign this block with.
     */
    fun sign(key: PrivateKey) {
        val payload = HalfBlockPayload.fromHalfBlock(this, sign = false).serialize()
        signature = key.sign(payload)
    }

    fun calculateHash(): ByteArray {
        val payload = HalfBlockPayload.fromHalfBlock(this).serialize()
        return sha256(payload)
    }

    class Builder(
        var type: String? = null,
        var rawTransaction: ByteArray? = null,
        var publicKey: ByteArray? = null,
        var sequenceNumber: UInt? = null,
        var linkPublicKey: ByteArray? = null,
        var linkSequenceNumber: UInt? = null,
        var previousHash: ByteArray? = null,
        var signature: ByteArray? = null
    ) {
        fun build(): TrustChainBlock {
            val type = type ?: throw IllegalStateException("type is null")
            val rawTransaction = rawTransaction ?: throw IllegalStateException("transaction is null")
            val publicKey = publicKey ?: throw IllegalStateException("public key is null")
            val sequenceNumber = sequenceNumber ?: throw IllegalStateException("sequence number is null")
            val linkPublicKey = linkPublicKey ?: throw IllegalStateException("link public key is null")
            val linkSequenceNumber = linkSequenceNumber ?: throw IllegalStateException("link sequence number is null")
            val previousHash = previousHash ?: throw IllegalStateException("previous hash is null")
            val signature = signature ?: throw IllegalStateException("signature is null")
            return TrustChainBlock(type,
                rawTransaction,
                publicKey,
                sequenceNumber,
                linkPublicKey,
                linkSequenceNumber,
                previousHash,
                signature,
                Date())
        }
    }
}
