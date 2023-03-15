package nl.tudelft.trustchain.detoks

data class RecipientPair(val publicKey: ByteArray, val proof: ByteArray) {

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as RecipientPair

        if (!publicKey.contentEquals(other.publicKey)) return false
        if (!proof.contentEquals(other.proof)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = publicKey.contentHashCode()
        result = 31 * result + proof.contentHashCode()
        return result
    }
}
