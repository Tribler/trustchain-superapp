package com.example.musicdao.repositories

import com.example.musicdao.ipv8.MusicCommunity
import com.example.musicdao.ipv8.SwarmHealth
import com.frostwire.jlibtorrent.SessionManager
import com.frostwire.jlibtorrent.Sha1Hash
import nl.tudelft.ipv8.android.IPv8Android


class SwarmHealthRepository(
    private val sessionManager: SessionManager,
    val musicCommunity: MusicCommunity
) {

    var mergedSwarmHealth: MutableMap<Sha1Hash, SwarmHealth> = mutableMapOf()

//    val localSwarmHealthMap: MutableMap<Sha1Hash, SwarmHealth>
//        get() = torrentRepository.swarmHealthMap

    val remoteSwarmHealthMap: MutableMap<Sha1Hash, SwarmHealth>
        get() = musicCommunity.swarmHealthMap

    /**
     * Merge local and remote swarm health map and remove outdated data
     */
    fun filterSwarmHealthMap(): MutableMap<Sha1Hash, SwarmHealth> {
        val localMap = updateLocalSwarmHealthMap()
        val musicCommunity = IPv8Android.getInstance().getOverlay<MusicCommunity>()
        val communityMap = musicCommunity?.swarmHealthMap ?: mutableMapOf()
        // Keep the highest numPeers/numSeeds count of all items in both maps
        // This map contains all the combined data, where local and community map data are merged;
        // the highest connectivity count for each item is saved in a gloal map for the MusicService
        val map: MutableMap<Sha1Hash, SwarmHealth> = mutableMapOf<Sha1Hash, SwarmHealth>()
        val allKeys = localMap.keys + communityMap.keys
        for (infoHash in allKeys) {
            val shLocal = localMap[infoHash]
            val shRemote = communityMap[infoHash]

            val bestSwarmHealth = SwarmHealth.pickBest(shLocal, shRemote)
            if (bestSwarmHealth != null) {
                map[infoHash] = bestSwarmHealth
            }
        }
        // Remove outdated swarm health data: if the data is outdated, we throw it away
        return map.filterValues { it.isUpToDate() }.toMutableMap()
    }

    /**
     * Go through all the torrents that we are currently seeding and mark its connectivity to peers
     */
    private fun updateLocalSwarmHealthMap(): MutableMap<Sha1Hash, SwarmHealth> {
//        val sessionManager = sessionManager
//        val contentSeeder = torrentRepository
//        val localMap = contentSeeder.swarmHealthMap
//        for (infoHash in localMap.keys) {
//            // Update all connectivity stats of the torrents that we are currently seeding
//            if (sessionManager.isRunning) {
//                val handle = sessionManager.find(infoHash) ?: continue
//                val newSwarmHealth = SwarmHealth(
//                    infoHash.toString(),
//                    handle.status().numPeers().toUInt(),
//                    handle.status().numSeeds().toUInt()
//                )
//                // Never go below 1, because we know we are at least 1 seeder of our local files
//                if (newSwarmHealth.numSeeds.toInt() < 1) continue
//                localMap[infoHash] = newSwarmHealth
//            }
//        }
//        return localMap
        return mutableMapOf()
    }

}
