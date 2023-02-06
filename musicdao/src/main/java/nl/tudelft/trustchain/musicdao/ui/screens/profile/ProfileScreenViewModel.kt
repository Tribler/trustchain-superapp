package nl.tudelft.trustchain.musicdao.ui.screens.profile

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import nl.tudelft.trustchain.musicdao.core.repositories.model.Album
import nl.tudelft.trustchain.musicdao.core.repositories.model.Artist
import nl.tudelft.trustchain.musicdao.core.repositories.ArtistRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

@RequiresApi(Build.VERSION_CODES.O)
class ProfileScreenViewModel @AssistedInject constructor(
    @Assisted private val publicKey: String,
    private val artistRepository: ArtistRepository
) : ViewModel() {

    private val _profile: MutableStateFlow<Artist?> = MutableStateFlow(null)
    var profile: StateFlow<Artist?> = _profile

    private val _releases: MutableStateFlow<List<Album>> = MutableStateFlow(listOf())
    val releases: StateFlow<List<Album>> = _releases

    init {
        viewModelScope.launch {
            profile = artistRepository.getArtistStateFlow(publicKey = publicKey)
            _releases.value = artistRepository.getArtistReleases(publicKey = publicKey)
        }
    }

    @AssistedFactory
    interface ProfileScreenViewModelFactory {
        fun create(publicKey: String): ProfileScreenViewModel
    }

    companion object {
        fun provideFactory(
            assistedFactory: ProfileScreenViewModelFactory,
            publicKey: String
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return assistedFactory.create(publicKey) as T
            }
        }
    }
}
