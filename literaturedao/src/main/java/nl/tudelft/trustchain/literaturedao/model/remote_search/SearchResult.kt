package nl.tudelft.trustchain.literaturedao.model.remote_search

import kotlinx.serialization.Serializable

@Serializable
data class SearchResult(val fileName: String, val score: Double, val magnetLink: String) {
}
