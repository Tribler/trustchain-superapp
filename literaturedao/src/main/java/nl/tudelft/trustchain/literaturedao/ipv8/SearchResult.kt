package nl.tudelft.trustchain.literaturedao.ipv8

import kotlinx.serialization.Serializable

@Serializable
data class SearchResult(val fileName: String, val score : Double, val magnetLink : String) {
}
