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

    private val _peerAmount: MutableLiveData<Int> = MutableLiveData()

    init {
        viewModelScope.launch {
            releases = albumRepository.getAlbumsFlow()
            _peerAmount.value = musicCommunity.getPeers().size
        }
    }
}
