package nl.tudelft.trustchain.musicdao.core.recommender.gossip

import androidx.annotation.VisibleForTesting
import com.frostwire.jlibtorrent.*
import kotlinx.coroutines.*
import mu.KotlinLogging
import nl.tudelft.trustchain.musicdao.core.recommender.graph.TrustNetwork
import nl.tudelft.trustchain.musicdao.core.recommender.model.NodeSongEdge
import nl.tudelft.trustchain.musicdao.core.recommender.model.NodeTrustEdge

/**
 * Gossips edges in the network to keep graphs synced
 */

lateinit var edgeGossiperInstance: EdgeGossiper
const val GOSSIP_DELAY: Long = 10000
const val DOWNLOAD_DELAY: Long = 20000

const val TORRENT_ATTEMPTS_THRESHOLD = 1
const val EVA_RETRIES = 10
private val logger = KotlinLogging.logger {}

class EdgeGossiper(
    private val recommenderCommunityBase: RecommenderCommunityBase,
    private val toastingEnabled: Boolean,
    private val trustNetwork: TrustNetwork
) {
    private val scope = CoroutineScope(Dispatchers.IO)
    private var sortedNodeToNodeEdges: List<NodeTrustEdge> = trustNetwork.getAllNodeToNodeEdges().sortedBy { it.timestamp }
    private var sortedNodeToSongEdges: List<NodeSongEdge> = trustNetwork.getAllNodeToSongEdges().sortedBy { it.timestamp }
    @VisibleForTesting(otherwise=VisibleForTesting.PRIVATE)
    var nodeToNodeEdgeDeltas = listOf<Int>()
    @VisibleForTesting(otherwise=VisibleForTesting.PRIVATE)
    var nodeToSongEdgeDeltas = listOf<Int>()
    @VisibleForTesting(otherwise=VisibleForTesting.PRIVATE)
    var nodeToNodeEdgeWeights = listOf<Float>()
    @VisibleForTesting(otherwise=VisibleForTesting.PRIVATE)
    var nodeToSongEdgeWeights = listOf<Float>()

    init {
        updateDeltasAndWeights()
    }

    companion object {
        fun getInstance(
            sessionManager: SessionManager,
            recommenderCommunity: RecommenderCommunityBase,
            toastingEnabled: Boolean = true,
            trustNetwork: TrustNetwork
        ): EdgeGossiper {
            if (!::edgeGossiperInstance.isInitialized) {
                edgeGossiperInstance = EdgeGossiper(recommenderCommunity, toastingEnabled, trustNetwork)
            }
            return edgeGossiperInstance
        }
    }

    private fun updateDeltasAndWeights() {
        updateNodeToNodeDeltas()
        updateNodeToNodeWeights()
        updateNodeToSongDeltas()
        updateNodeToSongWeights()
    }

    private fun updateNodeToNodeDeltas() {
        val oldestNodeToNodeEdgeTimestamp = sortedNodeToNodeEdges.first().timestamp.time
        val deltas = mutableListOf<Int>()
        for(edge in sortedNodeToSongEdges) {
            deltas.add((edge.timestamp.time - oldestNodeToNodeEdgeTimestamp).toInt())
        }
        nodeToNodeEdgeDeltas = deltas
    }

    private fun updateNodeToSongDeltas() {
        val oldestNodeToSongEdgeTimestamp = sortedNodeToSongEdges.first().timestamp.time
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

    private fun softmax(nums: List<Int>): List<Float> {
        val sum = nums.sum()
        return nums.map { it.toFloat() / sum }
    }


    fun start() {
        scope.launch {
            gossipNodeToNodeEdges()
        }
        scope.launch {
            gossipNodeToSongEdges()
        }
    }

    private suspend fun gossipNodeToSongEdges() {
        TODO("Not yet implemented")
    }

    private suspend fun gossipNodeToNodeEdges() {
        TODO("Not yet implemented")
    }
}
