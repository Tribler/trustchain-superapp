package nl.tudelft.trustchain.musicdao.core.recommender.gossip

import android.app.Service
import android.content.Intent
import android.os.Binder
import android.os.Build
import android.os.IBinder
import androidx.annotation.RequiresApi
import androidx.annotation.VisibleForTesting
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.*
import mu.KotlinLogging
import nl.tudelft.ipv8.Peer
import nl.tudelft.ipv8.android.IPv8Android
import nl.tudelft.trustchain.musicdao.CachePath
import nl.tudelft.trustchain.musicdao.core.ipv8.TrustedRecommenderCommunity
import nl.tudelft.trustchain.musicdao.core.recommender.model.*
import nl.tudelft.trustchain.musicdao.core.recommender.networks.SongRecTrustNetwork
import java.util.Random
import javax.inject.Inject
import kotlin.system.exitProcess

/**
 * Gossips edges in the network to keep graphs synced
 */

const val GOSSIP_DELAY: Long = 5000
const val N_EDGES_TO_GOSSIP: Int = 5

private val logger = KotlinLogging.logger {}

@AndroidEntryPoint
@RequiresApi(Build.VERSION_CODES.O)
class EdgeGossiperService(recCommunity: TrustedRecommenderCommunity? = null
): Service() {

    companion object {
        const val TIME_WINDOW: Int = 10000
    }

    @Inject
    lateinit var cachePath: CachePath

    /**
     * Class used for the client Binder.  Because we know this service always
     * runs in the same process as its clients, we don't need to deal with IPC.
     */
    inner class LocalBinder : Binder() {
        // Return this instance of LocalService so clients can call public methods
        fun getService(): EdgeGossiperService = this@EdgeGossiperService
    }

    private val binder = LocalBinder()
    private val scope = CoroutineScope(Dispatchers.IO)
    private val recCommunity = recCommunity ?: IPv8Android.getInstance().getOverlay<TrustedRecommenderCommunity>()!!
    private lateinit var trustNetwork: SongRecTrustNetwork
    private lateinit var sortedNodeToNodeEdges: List<NodeTrustEdge>
    private lateinit var sortedNodeToSongEdges: List<NodeSongEdge>
    @VisibleForTesting(otherwise=VisibleForTesting.PRIVATE)
    var nodeToNodeEdgeDeltas = listOf<Int>()
    @VisibleForTesting(otherwise=VisibleForTesting.PRIVATE)
    var nodeToSongEdgeDeltas = listOf<Int>()
    @VisibleForTesting(otherwise=VisibleForTesting.PRIVATE)
    var nodeToNodeEdgeWeights = listOf<Double>()
    @VisibleForTesting(otherwise=VisibleForTesting.PRIVATE)
    var nodeToSongEdgeWeights = listOf<Double>()
    @VisibleForTesting(otherwise=VisibleForTesting.PRIVATE)
    fun updateDeltasAndWeights(nodeToNodeEdges: List<NodeTrustEdge>, nodeToSongEdges: List<NodeSongEdge>) {
            sortedNodeToNodeEdges = nodeToNodeEdges.sortedBy { it.timestamp }.takeLast(TIME_WINDOW)
            sortedNodeToSongEdges = nodeToSongEdges.sortedBy { it.timestamp }.takeLast(TIME_WINDOW)
            if (sortedNodeToNodeEdges.isNotEmpty()) {
                updateNodeToNodeDeltas()
                updateNodeToNodeWeights()
            }
            if(sortedNodeToSongEdges.isNotEmpty()) {
                updateNodeToSongDeltas()
                updateNodeToSongWeights()
            }
    }

    private fun updateNodeToNodeDeltas() {
        val oldestNodeToNodeEdgeTimestamp = sortedNodeToNodeEdges.first().timestamp.time - (1000 * sortedNodeToNodeEdges.size)
        val deltas = mutableListOf<Int>()
        for(edge in sortedNodeToNodeEdges) {
            deltas.add((edge.timestamp.time - oldestNodeToNodeEdgeTimestamp).toInt())
        }
        nodeToNodeEdgeDeltas = deltas
    }

    private fun updateNodeToSongDeltas() {
        val oldestNodeToSongEdgeTimestamp = sortedNodeToSongEdges.first().timestamp.time - (1000 * sortedNodeToSongEdges.size)
        val deltas = mutableListOf<Int>()
        for(edge in sortedNodeToSongEdges) {
            deltas.add((edge.timestamp.time - oldestNodeToSongEdgeTimestamp).toInt())
        }
        nodeToSongEdgeDeltas = deltas
    }

    private fun updateNodeToNodeWeights() {
        nodeToNodeEdgeWeights = softmax(nodeToNodeEdgeDeltas)
    }

    private fun updateNodeToSongWeights() {
        nodeToSongEdgeWeights = softmax(nodeToSongEdgeDeltas)
    }

    private fun softmax(nums: List<Int>): List<Double> {
        val sum = nums.sum()
        return nums.map { it.toDouble() / sum }
    }


    fun start() {
        scope.launch {
            gossipEdges()
        }
    }
    private suspend fun gossipEdges() {
        while (scope.isActive) {
            if(!::trustNetwork.isInitialized) {
                trustNetwork = SongRecTrustNetwork.getInstance(IPv8Android.getInstance().getOverlay<TrustedRecommenderCommunity>()!!.myPeer.key.pub().toString(), cachePath.getPath().toString())
                sortedNodeToNodeEdges = trustNetwork.getAllNodeToNodeEdges().sortedBy { it.timestamp }.takeLast(TIME_WINDOW)
                sortedNodeToSongEdges = trustNetwork.getAllNodeToSongEdges().sortedBy { it.timestamp }.takeLast(TIME_WINDOW)
                updateDeltasAndWeights(trustNetwork.getAllNodeToNodeEdges(), trustNetwork.getAllNodeToSongEdges())
            }
            if(nodeToNodeEdgeDeltas.isNotEmpty() && nodeToSongEdgeDeltas.isNotEmpty()) {
                val randomPeer = pickRandomPeer()
                if (randomPeer != null) {
                    val nodeToNodeEdgesToGossip = pickRandomNodeToNodeEdgesToGossip()
                    recCommunity.sendNodeToNodeEdges(randomPeer, nodeToNodeEdgesToGossip)
                    val nodeRecEdgesToGossip = pickRandomNodeToSongEdgesToGossip()
                    recCommunity.sendNodeRecEdges(randomPeer, nodeRecEdgesToGossip)
                }
            }
            delay(GOSSIP_DELAY)
        }
    }

    private fun pickRandomNodeToNodeEdgesToGossip(): List<NodeTrustEdgeWithSourceAndTarget> {
        val p = Random().nextDouble()
        var cumP = 0.0
        var selectedNode: Node? = null
        run loop@{
            nodeToNodeEdgeWeights.forEachIndexed { index, d ->
                cumP += d
                if (p <= cumP) {
                    selectedNode = trustNetwork.nodeToNodeNetwork.graph.getEdgeSource(sortedNodeToNodeEdges[index])
                    return@loop
                }
            }
        }
        if(selectedNode != null) {
            val edgesToGossip = trustNetwork.nodeToNodeNetwork.graph.outgoingEdgesOf(selectedNode).sortedBy { it.trust }.takeLast(N_EDGES_TO_GOSSIP)
            return edgesToGossip.map { NodeTrustEdgeWithSourceAndTarget(it!!, selectedNode!!, trustNetwork.nodeToNodeNetwork.graph.getEdgeTarget(it))  }
        }
        return listOf()
    }

    private fun pickRandomNodeToSongEdgesToGossip(): List<NodeRecEdge> {
        val p = Random().nextDouble()
        var cumP = 0.0
        var selectedNode: Node? = null
        run loop@{
            nodeToSongEdgeWeights.forEachIndexed { index, d ->
                cumP += d
                if (p <= cumP) {
                    selectedNode = trustNetwork.nodeToSongNetwork.graph.getEdgeSource(sortedNodeToSongEdges[index]) as Node
                    return@loop
                }
            }
        }
        if(selectedNode != null) {
            val edgesToGossip = trustNetwork.nodeToSongNetwork.graph.outgoingEdgesOf(selectedNode).sortedBy { it.affinity }.takeLast(N_EDGES_TO_GOSSIP)
            return edgesToGossip.map { NodeRecEdge(it!!, selectedNode!!, trustNetwork.nodeToSongNetwork.graph.getEdgeTarget(it) as Recommendation)  }
        }
        return listOf()
    }

    private fun pickRandomPeer(): Peer? {
        val peers = recCommunity.getPeers()
        if (peers.isEmpty()) return null
        return peers.random()
    }

    override fun onBind(intent: Intent): IBinder {
        return binder
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return START_NOT_STICKY
    }

    override fun onCreate() {
        super.onCreate()
        scope.launch {
            start()
        }
    }

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
        // We need to kill the app as IPv8 is started in Application.onCreate
        exitProcess(0)
    }
}
