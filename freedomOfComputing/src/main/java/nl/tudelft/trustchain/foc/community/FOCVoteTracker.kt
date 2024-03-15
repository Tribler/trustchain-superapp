package nl.tudelft.trustchain.foc.community

import android.util.Log
import android.widget.Toast
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import nl.tudelft.ipv8.keyvault.PrivateKey
import nl.tudelft.trustchain.foc.MainActivityFOC
import nl.tudelft.trustchain.foc.util.ExtensionUtils
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.IOException
import java.io.ObjectInputStream
import java.io.ObjectOutputStream

class FOCVoteTracker(
    private val activity: MainActivityFOC,
    private val focCommunity: FOCCommunity
) {
    // Stores the votes for all apks
    private var voteMap: HashMap<String, HashSet<FOCSignedVote>> = HashMap()
    private val gossipDelay: Long = 1000
    private val scope = CoroutineScope(Dispatchers.IO)

    fun start() {
        scope.launch {
            iterativelyDownloadVotes()
        }
    }

    /**
     * Gets called on pause (or shutdown) of the app to persist state
     */
    fun storeState() {
        val votesFileName: String =
            activity.cacheDir.absolutePath + "/vote-tracker" + ExtensionUtils.DATA_DOT_EXTENSION
        try {
            File(votesFileName).writeBytes(serializeMap(voteMap))
        } catch (e: IOException) {
            printToast(e.toString())
            Log.e("vote-tracker-store", e.toString())
        }
    }

    /**
     * Gets called on start up to load the state from disk
     */
    fun loadState() {
        try {
            val voteFileName: String =
                activity.cacheDir.absolutePath + "/vote-tracker" + ExtensionUtils.DATA_DOT_EXTENSION
            val voteFile = File(voteFileName)

            if (voteFile.exists()) {
                voteMap = deserializeMap(voteFile.readBytes())
            }
        } catch (e: Exception) {
            printToast(e.toString())
            Log.e("vote-tracker load", e.toString())
        }
    }

    /**
     * Gets called when user places a vote
     * @param fileName APK on which vote is being placed
     * @param vote Vote that is being placed
     */
    fun vote(
        fileName: String,
        vote: FOCVote
    ) {
        // Sign the vote with the users private key such that other people can verify it
        val privateKey = focCommunity.myPeer.key as PrivateKey
        val signedVote = signVote(vote, privateKey)

        if (voteMap.containsKey(fileName)) {
            voteMap[fileName]!!.add(signedVote)
        } else {
            voteMap[fileName] = hashSetOf(signedVote)
        }
        focCommunity.informAboutVote(fileName, signedVote)
    }

    /**
     * Gets called when a vote from another user has to be added to our state
     * @param fileName APK on which vote is being placed
     * @param vote Vote that is being placed
     */
    private fun insertVote(
        fileName: String,
        vote: FOCSignedVote
    ) {
        // Check the signature of the vote
        // TODO should somehow check if pub-key is associated to person that placed the vote
        if (checkAndGet(vote) == null) {
            Log.w("vote-gossip", "received vote with invalid pub-key signature combination!")
            return
        }

        if (voteMap.containsKey(fileName)) {
            voteMap[fileName]!!.add(vote)
        } else {
            voteMap[fileName] = hashSetOf(vote)
        }
        activity.runOnUiThread {
            activity.updateVoteCounts(fileName)
        }
    }

    /**
     * Get the number of votes for an APK
     * @param fileName APK for which we want to know the number of votes
     * @param voteType vote type that is for or against the APK
     */
    fun getNumberOfVotes(
        fileName: String,
        voteType: VoteType
    ): Int {
        if (!voteMap.containsKey(fileName)) {
            return 0
        }
        return voteMap[fileName]!!.stream().map { checkAndGet(it) }.filter { v -> v?.voteType == voteType }.count().toInt()
    }

    /**
     * Method to take votes from the queue and insert them into the tracker
     */
    private suspend fun iterativelyDownloadVotes() {
        while (scope.isActive) {
            Log.i("vote-gossip", "${focCommunity.voteMessagesQueue.size} in Queue")
            while (!focCommunity.voteMessagesQueue.isEmpty()) {
                val (_, payload) = focCommunity.voteMessagesQueue.remove()
                insertVote(payload.fileName, payload.focSignedVote)
            }
            delay(gossipDelay)
        }
    }

    /**
     * Display a short message on the screen
     */
    private fun printToast(s: String) {
        Toast.makeText(activity.applicationContext, s, Toast.LENGTH_LONG).show()
    }

    private fun serializeMap(map: HashMap<String, HashSet<FOCSignedVote>>): ByteArray {
        val byteArrayOutputStream = ByteArrayOutputStream()
        val objectOutputStream = ObjectOutputStream(byteArrayOutputStream)
        objectOutputStream.writeObject(map)
        objectOutputStream.flush()
        return byteArrayOutputStream.toByteArray()
    }

    @Suppress("UNCHECKED_CAST")
    private fun deserializeMap(byteArray: ByteArray): HashMap<String, HashSet<FOCSignedVote>> {
        val byteArrayInputStream = ByteArrayInputStream(byteArray)
        val objectInputStream = ObjectInputStream(byteArrayInputStream)
        return objectInputStream.readObject() as HashMap<String, HashSet<FOCSignedVote>>
    }

    /**
     * Checks the signature of the signed vote and if the signature is correct and the signer is verified returns the vote object else returns null.
     */
    private fun checkAndGet(signedVote: FOCSignedVote): FOCVote? {
        // TODO Should somehow verify the pub-key is associated to a known user
        return signedVote.checkAndGet()
    }
}
