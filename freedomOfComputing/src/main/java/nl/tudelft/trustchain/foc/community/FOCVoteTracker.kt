package nl.tudelft.trustchain.foc.community

import android.util.Log
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.IOException
import java.io.ObjectInputStream
import java.io.ObjectOutputStream

object FOCVoteTracker {
    // Stores the votes for all apks
    private var voteMap: HashMap<String, HashSet<FOCSignedVote>> = HashMap()

    /**
     * Gets called on pause (or shutdown) of the app to persist state
     */
    fun storeState(fileName: String) {
        try {
            File(fileName).writeBytes(serializeMap(voteMap))
        } catch (e: IOException) {
            Log.e("vote-tracker-store", e.toString())
        }
    }

    /**
     * Gets called on start up to load the state from disk
     */
    fun loadState(fileName: String) {
        try {
            val voteFile = File(fileName)
            if (voteFile.exists()) {
                voteMap = deserializeMap(voteFile.readBytes())
            }
        } catch (e: Exception) {
            Log.e("vote-tracker load", e.toString())
        }
    }

    /**
     * Function to get the current state of votes
     */
    fun getCurrentState(): HashMap<String, HashSet<FOCVote>> {
        return voteMap
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
    }

    /**
     * Gets called when a user receives incoming voting data
     * @param incomingMap incoming data
     */
    fun mergeVoteMaps(incomingMap: HashMap<String, HashSet<FOCSignedVote>>) {
        for ((key, votes) in incomingMap) {
            if (voteMap.containsKey(key)) {
                voteMap[key]?.addAll(votes)
            } else { // this means votes from an apk can be received before the apk itself. Needs to be adjusted
                voteMap[key] = votes
            }
        }
        Log.i("pull based", "incoming map: $incomingMap")
        Log.i("pull based", "new voteMap :$voteMap ")
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
        return voteMap[fileName]!!.count { v -> v.vote.voteType == voteType }
    }

    fun createFileKey(fileName: String) {
        if (!voteMap.containsKey(fileName)) {
            voteMap[fileName] = HashSet()
        }
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
