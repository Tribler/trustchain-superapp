package nl.tudelft.trustchain.foc.community

import nl.tudelft.ipv8.keyvault.JavaCryptoProvider
import nl.tudelft.ipv8.keyvault.PrivateKey
import org.apache.commons.lang3.SerializationUtils
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class FOCSignedVoteTest {
    private lateinit var privateKey1: PrivateKey
    private lateinit var privateKey2: PrivateKey
    private val baseVote = FOCVote("0000", false)

    @Before
    fun setup() {
        privateKey1 = JavaCryptoProvider.keyFromPrivateBin(javaClass.getResource("/test_key1.bin")!!.readBytes())
        privateKey2 = JavaCryptoProvider.keyFromPrivateBin(javaClass.getResource("/test_key2.bin")!!.readBytes())
    }

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
