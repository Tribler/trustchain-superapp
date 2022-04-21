package nl.tudelft.trustchain.literaturedao.ipv8

import kotlinx.serialization.Serializable

@Serializable
data class SearchResultList(val results : List<SearchResult>) {
}
