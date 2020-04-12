package nl.tudelft.trustchain.trader.ui.transfer

import kotlinx.serialization.Serializable

@Serializable
data class SerializableBlock(
    val publicKey: String,
    val sequenceNumber: Int,
    val previousHash: ByteArray,
    val signature: ByteArray,
    val transaction: ByteArray,
    val timeStamp: Long
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as SerializableBlock

        if (publicKey != other.publicKey) return false
        if (sequenceNumber != other.sequenceNumber) return false
        if (!previousHash.contentEquals(other.previousHash)) return false
        if (!signature.contentEquals(other.signature)) return false
        if (!transaction.contentEquals(other.transaction)) return false
        if (timeStamp != other.timeStamp) return false

        return true
    }

    override fun hashCode(): Int {
        var result = publicKey.hashCode()
        result = 31 * result + sequenceNumber
        result = 31 * result + previousHash.contentHashCode()
        result = 31 * result + signature.contentHashCode()
        result = 31 * result + transaction.contentHashCode()
        result = 31 * result + timeStamp.hashCode()
        return result
    }
}
