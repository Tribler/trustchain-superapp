package nl.tudelft.trustchain.musicdao.core.recommender.model

import kotlinx.serialization.Serializable

@Serializable
data class Recommendations(
    val recommendations: Map<String, Float> = emptyMap(),
    var commonItems: Int = 0,
    var pcc: Float = 0.0f,
    var rdci: Float = 0.0f
)
