package nl.tudelft.trustchain.foc.community

import android.util.Log

class FOCVoteTracker {
    // Stores the votes for all apks
    private val voteMap: MutableMap<String, MutableSet<FOCVote>> = mutableMapOf()

    /**
     * Gets called on pause (or shutdown) of the app to persist state
     */
    fun storeState() {
        Log.w("vote-tracker", "storestate")
    }

    /**
     * Gets called on start up to load the state from disk
     */
    fun loadState() {
        Log.w("vote-tracker", "load state")
    }

    fun vote(fileName: String, vote: FOCVote) {
        if (voteMap.containsKey(fileName)) {
            val count = voteMap[fileName]!!.size
            Log.w("voting", "Size of set: $count")
            voteMap[fileName]!!.add(vote)
        } else {
            voteMap[fileName] = mutableSetOf(vote)
        }
    }
}
