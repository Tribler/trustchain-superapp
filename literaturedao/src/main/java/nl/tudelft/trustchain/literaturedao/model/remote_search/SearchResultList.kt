package nl.tudelft.trustchain.literaturedao.model.remote_search

import kotlinx.serialization.Serializable

@Serializable
data class SearchResultList(val results: List<SearchResult>) {
}
