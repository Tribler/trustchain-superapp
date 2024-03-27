package nl.tudelft.trustchain.community

import nl.tudelft.ipv8.keyvault.PrivateKey
import nl.tudelft.trustchain.foc.community.FOCVoteTracker
import nl.tudelft.trustchain.foc.community.FOCSignedVote
import nl.tudelft.trustchain.foc.community.FOCVote
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import nl.tudelft.ipv8.keyvault.JavaCryptoProvider
import org.apache.commons.lang3.SerializationUtils
import org.junit.After
import org.junit.Before
import java.io.File
import io.mockk.*
import android.util.Log

class FOCVoteTrackerTest {
    private val cryptoProvider = JavaCryptoProvider
    private var privateKey1: PrivateKey = cryptoProvider.generateKey()
    private var privateKey2: PrivateKey = cryptoProvider.generateKey()
    private val baseVote1 = FOCVote("0000", true)
    private val baseVote2 = FOCVote("0001", false)
    private var voteMap2: HashMap<String, HashSet<FOCSignedVote>> = HashMap()
    private val signKey1 = privateKey1.sign(SerializationUtils.serialize(baseVote1))
    private val signedVote1 = FOCSignedVote(baseVote1, signKey1, privateKey1.pub().keyToBin())
    private val signKey2 = privateKey2.sign(SerializationUtils.serialize(baseVote2))
    private val signedVote2 = FOCSignedVote(baseVote2, signKey2, privateKey2.pub().keyToBin())
    private val voteTracker: FOCVoteTracker = FOCVoteTracker

    @Before
    fun setup() {
        voteMap2["test.apk"] = HashSet()
        voteMap2["test.apk"]?.add(signedVote1)
        voteTracker.reset()
    }

    @After
    fun teardown() {
        // Delete the file after each test if it exists
        val file = File("test")
        if (file.exists()) {
            file.delete()
        }
    }

    @Test
    fun checkVoteAndCurrentStateCorrect() {
        voteTracker.vote("test.apk", signedVote1)
        assertEquals(voteMap2, voteTracker.getCurrentState())
    }

    @Test
    fun checkStoreStateCorrect() {
        voteTracker.vote("test.apk", signedVote1)
        voteTracker.storeState("test")
        val file = File("test")
        assertTrue(file.exists())
    }

    @Test
    fun voteWrongSignature() {
        mockkStatic(android.util.Log::class)
        every { Log.w(any(), any(String::class)) } returns 1
        val signedVote = FOCSignedVote(baseVote1, signKey1, privateKey2.pub().keyToBin())
        voteTracker.vote("test", signedVote)
        verify { Log.w("vote-gossip", "received vote with invalid pub-key signature combination!") }
    }

    @Test
    fun checkVoteDuplicate() {
        voteTracker.vote("test.apk", signedVote1)
        voteTracker.vote("test.apk", signedVote1)
        assertEquals(1, voteTracker.getNumberOfVotes("test.apk", true))
    }

    @Test
    fun checkLoadStateCorrect() {
        voteTracker.vote("test.apk", signedVote1)
        voteTracker.storeState("test")
        voteTracker.loadState("test")
        assertEquals(voteMap2, voteTracker.getCurrentState())
    }

    @Test
    fun checkMergeVoteMapsCorrect() {
        mockkStatic(android.util.Log::class)
        every { Log.i(any(), any()) } returns 1
        val voteMap3: HashMap<String, HashSet<FOCSignedVote>> = HashMap()
        voteMap3["test.apk"] = HashSet()
        voteMap3["test2.apk"] = HashSet()
        voteMap3["test.apk"]?.add(signedVote1)
        voteMap3["test2.apk"]?.add(signedVote2)
        voteTracker.vote("test2.apk", signedVote2)
        voteTracker.mergeVoteMaps(voteMap2)
        verify { Log.i("pull-based", "Merged maps") }
        assertEquals(voteMap3, voteTracker.getCurrentState())
    }

    @Test
    fun checkMergeVoteMapsCorrect2() {
        mockkStatic(android.util.Log::class)
        every { Log.i(any(), any()) } returns 1
        val voteMap3: HashMap<String, HashSet<FOCSignedVote>> = HashMap()
        voteMap3["test.apk"] = HashSet()
        voteMap3["test2.apk"] = HashSet()
        voteMap3["test.apk"]?.add(signedVote1)
        voteMap3["test2.apk"]?.add(signedVote2)
        voteTracker.vote("test2.apk", signedVote2)
        voteTracker.vote("test.apk", signedVote1)
        voteTracker.mergeVoteMaps(voteMap2)
        verify { Log.i("pull-based", "Merged maps") }
        assertEquals(voteMap3, voteTracker.getCurrentState())
    }

    @Test
    fun getNumberOfVotesCorrect() {
        voteTracker.vote("test.apk", signedVote1)
        assertEquals(0, voteTracker.getNumberOfVotes("test2.apk", true))
        assertEquals(1, voteTracker.getNumberOfVotes("test.apk", true))
    }
}
