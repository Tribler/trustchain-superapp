package nl.tudelft.trustchain.literaturedao.data_types

import kotlinx.serialization.Serializable

@Serializable
data class LocalData(
    val content: MutableList<Literature>
)
