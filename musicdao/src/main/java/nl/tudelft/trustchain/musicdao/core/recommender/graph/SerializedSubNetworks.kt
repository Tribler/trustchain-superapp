package nl.tudelft.trustchain.musicdao.core.recommender.graph

import kotlinx.serialization.Serializable

@Serializable
data class SerializedSubNetworks(
    val nodeToNodeNetworkSerialized: String,
    val nodeToSongNetworkSerialized: String
)
