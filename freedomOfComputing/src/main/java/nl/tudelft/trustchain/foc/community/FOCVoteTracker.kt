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
    private var voteMap: HashMap<String, HashSet<FOCVote>> = HashMap()

    /**
     * Gets called on pause (or shutdown) of the app to persist state
     */
    fun storeState() {
        val fileName: String =
            activity.applicationContext.cacheDir.absolutePath + "/vote-tracker" + ExtensionUtils.DATA_DOT_EXTENSION
        try {
            val file = File(fileName)
            file.writeBytes(serializeMap(voteMap))
        } catch (e: IOException) {
            this.printToast(e.toString())
            Log.e("vote-tracker", e.toString())
        }
    }

    /**
     * Gets called on start up to load the state from disk
     */
    fun loadState() {
        try {
            val fileName: String =
                activity.applicationContext.cacheDir.absolutePath + "/vote-tracker" + ExtensionUtils.DATA_DOT_EXTENSION
            val file = File(fileName)
            if (file.exists()) {
                voteMap = deserializeMap(file.readBytes())
            }
        } catch (e: Exception) {
            this.printToast(e.toString())
            Log.e("vote-tracker", e.toString())
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
        if (voteMap.containsKey(fileName)) {
            val count = voteMap[fileName]!!.size
            Log.w("voting", "Size of set: $count")
            voteMap[fileName]!!.add(vote)
        } else {
            voteMap[fileName] = hashSetOf(vote)
        }
    }

    /**
     * Get the number of votes for an APK
     * @param fileName APK for which we want to know the number of votes
     */
    fun getNumberOfVotes(fileName: String): Int {
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
