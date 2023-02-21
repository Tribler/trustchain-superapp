package nl.tudelft.trustchain.musicdao.ui.screens.artists

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import nl.tudelft.trustchain.musicdao.core.repositories.model.Artist
import nl.tudelft.trustchain.musicdao.core.repositories.ArtistRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@RequiresApi(Build.VERSION_CODES.O)
@HiltViewModel
class DiscoverArtistsViewModel @Inject constructor(
    private val artistRepository: ArtistRepository,
) : ViewModel() {

    private val _artists: MutableStateFlow<List<Artist>> = MutableStateFlow(listOf())
    val artists: StateFlow<List<Artist>> = _artists

    private val _isRefreshing: MutableLiveData<Boolean> = MutableLiveData()
    val isRefreshing: LiveData<Boolean> = _isRefreshing

    init {
        viewModelScope.launch {
            _artists.value = artistRepository.getArtists()
        }
    }

    fun refresh() {
        viewModelScope.launch {
            _isRefreshing.value = true
            delay(500)
            _artists.value = artistRepository.getArtists()
            _isRefreshing.value = false
        }
    }
}
