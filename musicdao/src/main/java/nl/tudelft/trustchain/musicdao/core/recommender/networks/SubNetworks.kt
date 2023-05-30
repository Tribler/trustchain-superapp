package nl.tudelft.trustchain.musicdao.core.recommender.networks

import nl.tudelft.trustchain.musicdao.core.recommender.graph.NodeToNodeNetwork
import nl.tudelft.trustchain.musicdao.core.recommender.graph.NodeToSongNetwork

data class SubNetworks(
    val nodeToNodeNetwork: NodeToNodeNetwork,
    val nodeToSongNetwork: NodeToSongNetwork
)
