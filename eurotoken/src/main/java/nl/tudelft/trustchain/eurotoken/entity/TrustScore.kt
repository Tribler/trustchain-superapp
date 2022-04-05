package nl.tudelft.trustchain.eurotoken.entity

/**
 * The [TrustScore] of a peer by public key.
 */
data class TrustScore (
    val pubKey : ByteArray,
    val trust : Int
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as TrustScore

        if (!pubKey.contentEquals(other.pubKey)) return false
        if (trust != other.trust) return false

        return true
    }

    override fun hashCode(): Int {
        var result = pubKey.contentHashCode()
        result = 31 * result + trust
        return result
    }
}
