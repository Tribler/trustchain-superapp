package nl.tudelft.trustchain.musicdao.core.recommender.collaborativefiltering

import nl.tudelft.trustchain.musicdao.core.recommender.networks.TrustNetwork
import nl.tudelft.trustchain.musicdao.core.recommender.model.*
import java.lang.Float.POSITIVE_INFINITY
import kotlin.math.sqrt

class UserBasedTrustedCollaborativeFiltering(
    var sortedTopTrustedUsers: List<Node>,
    val trustNetwork: TrustNetwork,
    val a: Double,
    val b: Double
) : CollaborativeFiltering {
    companion object {
        private lateinit var instance: UserBasedTrustedCollaborativeFiltering
        fun getInstance(
            topTrustedUsers: List<Node>,
            trustNetwork: TrustNetwork,
            a: Double,
            b: Double
        ): UserBasedTrustedCollaborativeFiltering {
            if (!Companion::instance.isInitialized) {
                instance = UserBasedTrustedCollaborativeFiltering(topTrustedUsers, trustNetwork, a, b)
            }
            return instance
        }
    }

    override fun similarNodes(nodeToSongEdges: List<NodeRecEdge>, size: Int): Map<Node, Double> {
        val sourceAffinities = nodeToSongEdges.associateBy({ it.rec }, { it.nodeSongEdge.affinity })
        val nodeSimilarities = mutableListOf<NodeSimilarity>()
        for (node in sortedTopTrustedUsers) {
            val targetSongEdges = trustNetwork.nodeToSongNetwork.graph.outgoingEdgesOf(node)
            val targetAffinities =
                targetSongEdges.associateBy(
                    { trustNetwork.nodeToSongNetwork.graph.getEdgeTarget(it) as Recommendation },
                    { it.affinity }
                )
            val commonItems = sourceAffinities.keys.intersect(targetAffinities.keys)
            val nodeSimilarity = NodeSimilarity(node)
            if (commonItems.isNotEmpty()) {
                nodeSimilarity.pcc = calculatePearsonCorrelationCoefficient(sourceAffinities, targetAffinities, commonItems)
                nodeSimilarity.rdci = calculateRatingDifferenceOfCommonItem(sourceAffinities, targetAffinities, commonItems)
                nodeSimilarity.commonItems = commonItems.size
            }
            nodeSimilarities.add(nodeSimilarity)
        }
        val maxCommonItems = nodeSimilarities.map { it.commonItems }.maxOrNull()
        return if (maxCommonItems == null || maxCommonItems == 0) {
            sortedTopTrustedUsers.takeLast(size).associateBy({ it }, { it.getPersonalizedPageRankScore() })
        } else {
            val nodesTrustAndSimilarity = mutableListOf<Pair<Node, Double>>()
            for (nodeSim in nodeSimilarities) {
                val cf = nodeSim.commonItems.toDouble() / maxCommonItems
                val nSim = cf * nodeSim.pcc
                val similarity = (nSim * b) + (nodeSim.rdci * (1 - b))
                val combinedTrustAndSimilarity =
                    (nodeSim.node.getPersonalizedPageRankScore() * a) + ((similarity + 1) * (1 - a))
                nodesTrustAndSimilarity.add(Pair(nodeSim.node, combinedTrustAndSimilarity))
            }
            nodesTrustAndSimilarity.sortedBy { it.second }.takeLast(size).associateBy({ it.first }, { it.second })
        }
    }

    private fun calculateRatingDifferenceOfCommonItem(
        sourceAffinities: Map<Recommendation, Double>,
        targetAffinities: Map<Recommendation, Double>,
        commonItems: Set<Recommendation>
    ): Double {
        var maxRating = 0.0
        var minRating = POSITIVE_INFINITY.toDouble()
        val cumRatingDifference = commonItems.map {
            val largerRating = maxOf(sourceAffinities[it]!!, targetAffinities[it]!!)
            val smallerRating = minOf(sourceAffinities[it]!!, targetAffinities[it]!!)
            if (largerRating > maxRating) maxRating = largerRating
            if (smallerRating < minRating) minRating = smallerRating
            largerRating - smallerRating
        }.sum()
        val ratingDifferenceFactor = cumRatingDifference / commonItems.size
        val ratingSpan = maxRating - minRating
        return if (ratingSpan == 0.0) 0.0
        else 1 - (ratingDifferenceFactor * (1 / ratingSpan))
    }

    private fun calculatePearsonCorrelationCoefficient(
        sourceAffinities: Map<Recommendation, Double>,
        targetAffinities: Map<Recommendation, Double>,
        commonItems: Set<Recommendation>
    ): Double {
        val sourceAvg = sourceAffinities.values.average()
        val targetAvg = targetAffinities.values.average()
        val sourceVariances = commonItems.associateBy({ it }, { sourceAffinities[it]!! - sourceAvg })
        val targetVariances = commonItems.associateBy({ it }, { targetAffinities[it]!! - targetAvg })
        val numerator = commonItems.sumOf { song ->
            sourceVariances[song]!! * targetVariances[song]!!
        }

        return if (numerator != 0.0) {
            val sourceDenominator = commonItems.sumOf { song ->
                sqrt(sourceVariances[song]!! * sourceVariances[song]!!)
            }
            val targetDenominator = commonItems.sumOf { song ->
                sqrt(targetVariances[song]!! * targetVariances[song]!!)
            }
            val denominator = sourceDenominator * targetDenominator
            numerator / denominator
        } else {
            numerator
        }
    }
}
