package nl.tudelft.trustchain.musicdao.ui.screens.search

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import nl.tudelft.trustchain.musicdao.core.ipv8.MusicCommunity
import nl.tudelft.trustchain.musicdao.core.repositories.AlbumRepository
import nl.tudelft.trustchain.musicdao.core.repositories.model.Album
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@RequiresApi(Build.VERSION_CODES.O)
@HiltViewModel
class SearchScreenViewModel @Inject constructor(
    private val albumRepository: AlbumRepository,
    private val musicCommunity: MusicCommunity
) : ViewModel() {

    private val _isRefreshing: MutableLiveData<Boolean> = MutableLiveData()
    val isRefreshing: LiveData<Boolean> = _isRefreshing

    private val _searchQuery: MutableStateFlow<String> = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery

    private val _searchResult: MutableStateFlow<List<Album>> = MutableStateFlow(listOf())
    val searchResult: StateFlow<List<Album>> = _searchResult

    private val _peerAmount: MutableLiveData<Int> = MutableLiveData()
    var peerAmount: LiveData<Int> = _peerAmount

    private val _totalReleaseAmount: MutableLiveData<Int> = MutableLiveData()
    var totalReleaseAmount: LiveData<Int> = _totalReleaseAmount

    private var searchJob: Job? = null

    init {
        viewModelScope.launch {
            _searchResult.value = downloadedFirstInListOfAlbums(albumRepository.getAlbums())
            _peerAmount.value = musicCommunity.getPeers().size
            _totalReleaseAmount.value = albumRepository.getAlbums().size
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    fun searchDebounced(searchText: String) {
        _searchQuery.value = searchText

        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            delay(DEBOUNCE_DELAY)
            search(searchText)
        }
    }

    fun downloadedFirstInListOfAlbums(list: List<Album>): List<Album> {
        // put downloaded albums first
        val downloadedAlbums = list.sortedBy { album ->
            album.songs != null && album.songs.isNotEmpty()
        }.reversed()
        return downloadedAlbums
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private suspend fun search(searchText: String) {
        if (searchText.isEmpty()) {
            _searchResult.value = downloadedFirstInListOfAlbums(albumRepository.getAlbums())
        } else {
            val result = albumRepository.searchAlbums(searchText)
            _searchResult.value = downloadedFirstInListOfAlbums(result)
        }
    }

    fun refresh() {
        viewModelScope.launch {
            _isRefreshing.value = true
            delay(500)
            if (_searchQuery.value.isEmpty()) {
                _searchResult.value = downloadedFirstInListOfAlbums(albumRepository.getAlbums())
            }
            _peerAmount.value = musicCommunity.getPeers().size
            _totalReleaseAmount.value = albumRepository.getAlbums().size
            _isRefreshing.value = false
        }
    }

    companion object {
        private const val DEBOUNCE_DELAY = 200L
    }
}
