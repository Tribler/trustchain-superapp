package nl.tudelft.trustchain.musicdao.ui.screens.home

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
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import javax.inject.Inject

@RequiresApi(Build.VERSION_CODES.O)
@HiltViewModel
class HomeScreenViewModel @Inject constructor(
    private val albumRepository: AlbumRepository,
    private val musicCommunity: MusicCommunity
) : ViewModel() {

    private val _releases: MutableLiveData<List<Album>> = MutableLiveData()
    var releases: LiveData<List<Album>> = _releases

    private val _isRefreshing: MutableLiveData<Boolean> = MutableLiveData()
    val isRefreshing: LiveData<Boolean> = _isRefreshing

    private val _peerAmount: MutableLiveData<Int> = MutableLiveData()
    var peerAmount: LiveData<Int> = _peerAmount

    init {
        viewModelScope.launch {
            releases = albumRepository.getAlbumsFlow()
            _peerAmount.value = musicCommunity.getPeers().size
        }
    }

    fun refresh() {
        viewModelScope.launch {
            _isRefreshing.value = true
            delay(500)
            _releases.value = albumRepository.getAlbums()
            _peerAmount.value = musicCommunity.getPeers().size
            _isRefreshing.value = false
        }
    }
}
