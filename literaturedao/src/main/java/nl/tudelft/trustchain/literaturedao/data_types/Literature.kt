package nl.tudelft.trustchain.literaturedao.data_types

import kotlinx.serialization.Serializable

@Serializable
data class Literature(
    val title: String,
    val magnet: String,
    val keywords: MutableList<Pair<String, Double>>,
    val local: Boolean,
    val date: String,
    val localFileUri: String
)
