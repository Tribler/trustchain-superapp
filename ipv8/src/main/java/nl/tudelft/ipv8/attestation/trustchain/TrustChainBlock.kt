package nl.tudelft.ipv8.attestation.trustchain

import java.util.*

val GENESIS_HASH = ByteArray(32)
val GENESIS_SEQ = 1
val UNKNOWN_SEQ = 0
val EMPTY_SIG = ByteArray(64)
val EMPTY_PK = ByteArray(74)

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
    val signature: ByteArray,

    /**
     * The time when this block was created.
     */
    val timestamp: Date,

    /**
     * The time when this block was inserted into the local database, if this block was inserted into
     * the local database.
     */
    val insertTime: Date? = null
)
