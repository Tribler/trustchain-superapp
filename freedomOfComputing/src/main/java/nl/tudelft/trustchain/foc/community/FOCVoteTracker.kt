package nl.tudelft.trustchain.foc.community

import android.util.Log
import android.widget.Toast
import nl.tudelft.trustchain.foc.MainActivityFOC
import nl.tudelft.trustchain.foc.util.ExtensionUtils
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.IOException
import java.io.ObjectInputStream
import java.io.ObjectOutputStream

class FOCVoteTracker(private val activity: MainActivityFOC) {
    // Stores the votes for all apks
    private var upVoteMap: HashMap<String, HashSet<FOCVote>> = HashMap()
    private var downVoteMap: HashMap<String, HashSet<FOCVote>> = HashMap()

    /**
     * Gets called on pause (or shutdown) of the app to persist state
     */
    fun storeState() {
        val upVotesFileName: String =
            activity.applicationContext.cacheDir.absolutePath + "/vote-tracker-upvotes" + ExtensionUtils.DATA_DOT_EXTENSION
        val downVotesFileName: String =
            activity.applicationContext.cacheDir.absolutePath + "/vote-tracker-downvotes" + ExtensionUtils.DATA_DOT_EXTENSION
        try {
            File(upVotesFileName).writeBytes(serializeMap(upVoteMap))
            File(downVotesFileName).writeBytes(serializeMap(downVoteMap))
        } catch (e: IOException) {
            this.printToast(e.toString())
            Log.e("vote-tracker-store", e.toString())
        }
    }

    /**
     * Gets called on start up to load the state from disk
     */
    fun loadState() {
        try {
            val upVoteFileName: String =
                activity.applicationContext.cacheDir.absolutePath + "/vote-tracker-upvotes" + ExtensionUtils.DATA_DOT_EXTENSION
            val downVoteFileName: String =
                activity.applicationContext.cacheDir.absolutePath + "/vote-tracker-downvotes" + ExtensionUtils.DATA_DOT_EXTENSION
            val upVoteFile = File(upVoteFileName)
            val downVoteFile = File(downVoteFileName)

            if (upVoteFile.exists()) {
                upVoteMap = deserializeMap(upVoteFile.readBytes())
            }
            if (downVoteFile.exists()) {
                downVoteMap = deserializeMap(downVoteFile.readBytes())
            }
        } catch (e: Exception) {
            this.printToast(e.toString())
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
        val voteMap = if (vote.voteType == 1) upVoteMap else downVoteMap
        if (voteMap.containsKey(fileName)) {
            val count = voteMap[fileName]!!.size
            val tag = if (vote.voteType == 1) "upvote" else "downvote"
            Log.w(tag, "Size of set: $count")
            voteMap[fileName]!!.add(vote)
        } else {
            voteMap[fileName] = hashSetOf(vote)
        }
    }

    /**
     * Get the number of votes for an APK
     * @param fileName APK for which we want to know the number of votes
     * @param voteType vote type that is for or against the APK
     */
    fun getNumberOfVotes(
        fileName: String,
        voteType: Int
    ): Int {
        val voteMap = if (voteType == 1) upVoteMap else downVoteMap
        if (!voteMap.containsKey(fileName)) {
            return 0
        }
        return voteMap[fileName]!!.size
    }

    /**
     * Display a short message on the screen
     */
    private fun printToast(s: String) {
        Toast.makeText(activity.applicationContext, s, Toast.LENGTH_LONG).show()
    }

    private fun serializeMap(map: HashMap<String, HashSet<FOCVote>>): ByteArray {
        val byteArrayOutputStream = ByteArrayOutputStream()
        val objectOutputStream = ObjectOutputStream(byteArrayOutputStream)
        objectOutputStream.writeObject(map)
        objectOutputStream.flush()
        return byteArrayOutputStream.toByteArray()
    }

    @Suppress("UNCHECKED_CAST")
    private fun deserializeMap(byteArray: ByteArray): HashMap<String, HashSet<FOCVote>> {
        val byteArrayInputStream = ByteArrayInputStream(byteArray)
        val objectInputStream = ObjectInputStream(byteArrayInputStream)
        return objectInputStream.readObject() as HashMap<String, HashSet<FOCVote>>
    }
}
