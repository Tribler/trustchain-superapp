package nl.tudelft.trustchain.musicdao.ui.screens.search

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import nl.tudelft.trustchain.musicdao.core.repositories.AlbumRepository
import nl.tudelft.trustchain.musicdao.core.repositories.model.Album
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import nl.tudelft.ipv8.android.IPv8Android
import nl.tudelft.trustchain.musicdao.CachePath
import nl.tudelft.trustchain.musicdao.core.ipv8.TrustedRecommenderCommunity
import nl.tudelft.trustchain.musicdao.core.recommender.model.Recommendation
import nl.tudelft.trustchain.musicdao.core.recommender.networks.SongRecTrustNetwork
import javax.inject.Inject

@RequiresApi(Build.VERSION_CODES.O)
@HiltViewModel
class DiscoverScreenViewModel @Inject constructor(
    private val albumRepository: AlbumRepository,
    val cachePath: CachePath
) : ViewModel() {

    private val _isRefreshing: MutableLiveData<Boolean> = MutableLiveData()
    val isRefreshing: LiveData<Boolean> = _isRefreshing

    private val _recommendations: MutableStateFlow<List<Album>> = MutableStateFlow(listOf())
    val recommendations: StateFlow<List<Album>> = _recommendations

    private val _peerAmount: MutableLiveData<Int> = MutableLiveData()
    var peerAmount: LiveData<Int> = _peerAmount

    private val recCommunity = IPv8Android.getInstance().getOverlay<TrustedRecommenderCommunity>()!!

    private lateinit var songRecTrustNetwork: SongRecTrustNetwork

    init {
        viewModelScope.launch {
            val allAlbums = albumRepository.getAlbums()
            songRecTrustNetwork = SongRecTrustNetwork.getInstance(recCommunity.myPeer.key.pub().toString(), cachePath.getPath().toString())
            _recommendations.value = ratingOfAlbumNonDownloadedSong(allAlbums)
            _peerAmount.value = recCommunity.getPeers().size
        }
    }

    private fun ratingOfAlbumNonDownloadedSong(allAlbums: List<Album>): List<Album> {
        val allRecs = songRecTrustNetwork.nodeToSongNetwork.getAllSongs()
        return allAlbums.filter { allRecs.contains(Recommendation(it.id)) }
            .sortedBy { allRecs.find { rec -> rec.identifier == it.id }!!.rankingScore }.reversed()
    }

    fun downloadedFirstInListOfAlbums(list: List<Album>): List<Album> {
        // put downloaded albums first
        val downloadedAlbums = list.sortedBy { album ->
            album.songs != null && album.songs.isNotEmpty()
        }.reversed()
        return downloadedAlbums
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private suspend fun recommend(searchText: String) {
        val result = albumRepository.searchAlbums(searchText)
        _recommendations.value = downloadedFirstInListOfAlbums(result)
    }

    fun refresh() {
        viewModelScope.launch {
            _isRefreshing.value = true
            delay(500)
            _peerAmount.value = recCommunity.getPeers().size
            _isRefreshing.value = false
            songRecTrustNetwork.refreshRecommendations()
            val allAlbums = albumRepository.getAlbums()
            _recommendations.value = ratingOfAlbumNonDownloadedSong(allAlbums)
        }
    }

    companion object {
        private const val DEBOUNCE_DELAY = 200L
    }
}
