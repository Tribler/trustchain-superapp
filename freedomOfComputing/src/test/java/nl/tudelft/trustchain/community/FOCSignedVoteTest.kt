package nl.tudelft.trustchain.community

import nl.tudelft.ipv8.keyvault.JavaCryptoProvider
import nl.tudelft.ipv8.keyvault.PrivateKey
import nl.tudelft.trustchain.foc.community.FOCSignedVote
import nl.tudelft.trustchain.foc.community.FOCVote
import nl.tudelft.trustchain.foc.community.signVote
import org.apache.commons.lang3.SerializationUtils
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class FOCSignedVoteTest {
    private val cryptoProvider = JavaCryptoProvider
    private var privateKey1: PrivateKey = cryptoProvider.generateKey()
    private var privateKey2: PrivateKey = cryptoProvider.generateKey()
    private val baseVote = FOCVote("0000", false)

    @Test
    fun checkSignatureCorrect() {
        val signedVote = signVote(baseVote, privateKey1)

        assertTrue(signedVote.checkSignature())
    }

    @Test
    fun checkSignatureIncorrect() {
        val signKey2 = privateKey2.sign(SerializationUtils.serialize(baseVote))
        // We create a vote that is signed wit private key 1 but public key 2 is attached
        val signedVote = FOCSignedVote(baseVote, signKey2, privateKey1.pub().keyToBin())

        assertFalse(signedVote.checkSignature())
    }

    @Test
    fun checkAndGetCorrect() {
        val signedVote = signVote(baseVote, privateKey1)

        assertEquals(baseVote, signedVote.checkAndGet())
    }

    @Test
    fun checkAndGetWrong() {
        val signKey2 = privateKey2.sign(SerializationUtils.serialize(baseVote))
        // We create a vote that is signed wit private key 1 but public key 2 is attached
        val signedVote = FOCSignedVote(baseVote, signKey2, privateKey1.pub().keyToBin())

        assertNull(signedVote.checkAndGet())
    }
}
