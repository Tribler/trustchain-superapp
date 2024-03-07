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
}
