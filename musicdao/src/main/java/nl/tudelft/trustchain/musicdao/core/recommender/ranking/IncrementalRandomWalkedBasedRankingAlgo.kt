package nl.tudelft.trustchain.musicdao.core.recommender.ranking

import nl.tudelft.trustchain.musicdao.core.recommender.model.Node
import nl.tudelft.trustchain.musicdao.core.recommender.model.NodeOrSong
import org.jgrapht.graph.AbstractBaseGraph
import org.jgrapht.graph.DefaultWeightedEdge

abstract class IncrementalRandomWalkedBasedRankingAlgo<G, V, E>(
    protected val maxWalkLength: Int,
    protected val repetitions: Int,
    protected val rootNode: Node
) where G: AbstractBaseGraph<V, E>, V: NodeOrSong, E: DefaultWeightedEdge {

    abstract fun calculateRankings()

    abstract fun initiateRandomWalks()
}
