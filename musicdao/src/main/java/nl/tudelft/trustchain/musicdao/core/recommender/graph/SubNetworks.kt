package nl.tudelft.trustchain.musicdao.core.recommender.graph

data class SubNetworks(
    val nodeToNodeNetwork: NodeToNodeNetwork,
    val nodeToSongNetwork: NodeToSongNetwork
)
