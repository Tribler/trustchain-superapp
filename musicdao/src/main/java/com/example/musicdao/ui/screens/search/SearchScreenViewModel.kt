package com.example.musicdao.ui.screens.search

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.musicdao.core.model.Album
import com.example.musicdao.core.usecases.releases.SearchAlbums
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SearchScreenViewModel @Inject constructor(
    private val searchAlbums: SearchAlbums
) : ViewModel() {

    private val _searchQuery: MutableStateFlow<String> = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery

    private val _searchResult: MutableStateFlow<List<Album>> = MutableStateFlow(listOf())
    val searchResult: StateFlow<List<Album>> = _searchResult

    private var searchJob: Job? = null

    @RequiresApi(Build.VERSION_CODES.O)
    fun searchDebounced(searchText: String) {
        _searchQuery.value = searchText

        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            delay(DEBOUNCE_DELAY)
            search(searchText)
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private suspend fun search(searchText: String) {
        if (searchText.isEmpty()) {
            _searchResult.value = listOf()
        } else {
            val result = searchAlbums.invoke(searchText)
            _searchResult.value = result
        }
    }

    companion object {
        private val DEBOUNCE_DELAY = 200L
    }
}
