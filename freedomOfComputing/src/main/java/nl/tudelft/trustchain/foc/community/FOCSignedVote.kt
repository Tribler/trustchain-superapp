package nl.tudelft.trustchain.foc.community

import nl.tudelft.ipv8.keyvault.PrivateKey
import nl.tudelft.ipv8.keyvault.PublicKey
import java.io.Serializable
import org.apache.commons.lang3.SerializationUtils

fun signVote(
    vote: FOCVote,
    myPrivateKey: PrivateKey,
    publicKey: PublicKey
): FOCSignedVote {
    val voteSerialized = SerializationUtils.serialize(vote)
    val signature = myPrivateKey.sign(voteSerialized)
    return FOCSignedVote(vote, signature, publicKey)
}

class FOCSignedVote(val vote: FOCVote, private val signature: ByteArray, val publicKey: PublicKey) : Serializable {
    fun checkSignature(): Boolean {
        val voteSerialized = SerializationUtils.serialize(vote)
        return publicKey.verify(signature, voteSerialized)
    }

    fun checkAndGet(): FOCVote? {
        return if (checkSignature()) {
            vote
        } else {
            null
        }
    }
}
