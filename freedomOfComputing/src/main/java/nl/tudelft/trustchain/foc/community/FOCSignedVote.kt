package nl.tudelft.trustchain.foc.community

import nl.tudelft.ipv8.keyvault.defaultCryptoProvider
import nl.tudelft.ipv8.keyvault.PrivateKey
import nl.tudelft.ipv8.keyvault.PublicKey
import java.io.Serializable
import org.apache.commons.lang3.SerializationUtils

fun signVote(
    vote: FOCVote,
    myPrivateKey: PrivateKey
): FOCSignedVote {
    val pubKey = myPrivateKey.pub()
    val voteSerialized = SerializationUtils.serialize(vote)
    val signature = myPrivateKey.sign(voteSerialized)
    return FOCSignedVote(vote, signature, pubKey.keyToBin())
}

/**
 * Wrapper class that holds the regular vote objects this class contains the signature of each vote
 */
data class FOCSignedVote(val vote: FOCVote, private val signature: ByteArray, val publicKeyBin: ByteArray) : Serializable {
    /**
     * Constructs a [PublicKey] and returns it
     */
    fun getPublicKey(): PublicKey {
        return defaultCryptoProvider.keyFromPublicBin(publicKeyBin)
    }

    /**
     * Verifies the [signature] with the [publicKeyBin] and returns true if the vote was signed correctly
     */
    fun checkSignature(): Boolean {
        val voteSerialized = SerializationUtils.serialize(vote)
        val publicKey = getPublicKey()
        return publicKey.verify(signature, voteSerialized)
    }

    /**
     * Checks to see if the signature is correct and returns the [vote] object, otherwise returns null
     */
    fun checkAndGet(): FOCVote? {
        return if (checkSignature()) {
            vote
        } else {
            null
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as FOCSignedVote

        if (vote != other.vote) return false
        if (!signature.contentEquals(other.signature)) return false
        if (!publicKeyBin.contentEquals(other.publicKeyBin)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = vote.hashCode()
        result = 31 * result + signature.contentHashCode()
        result = 31 * result + publicKeyBin.contentHashCode()
        return result
    }
}
