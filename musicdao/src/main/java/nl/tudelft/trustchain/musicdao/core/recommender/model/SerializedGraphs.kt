package nl.tudelft.trustchain.musicdao.core.recommender.model

data class SerializedGraphs(
    val nodeToNodeNetwork: String,
    var nodeToSongNetwork: String
)
