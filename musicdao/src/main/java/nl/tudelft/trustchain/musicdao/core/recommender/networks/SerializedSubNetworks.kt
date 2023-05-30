package nl.tudelft.trustchain.musicdao.core.recommender.networks

import kotlinx.serialization.Serializable

@Serializable
data class SerializedSubNetworks(
    val nodeToNodeNetworkSerialized: String,
    val nodeToSongNetworkSerialized: String
)
