package nl.tudelft.trustchain.musicdao.ui.screens.donate

import androidx.lifecycle.ViewModel
import nl.tudelft.trustchain.musicdao.core.repositories.ArtistRepository
import nl.tudelft.trustchain.musicdao.core.repositories.model.Artist
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

@HiltViewModel
class DonateScreenViewModel
    @Inject
    constructor(val artistRepository: ArtistRepository) : ViewModel() {
        var artist: StateFlow<Artist?> = MutableStateFlow(null)

        suspend fun setArtist(publicKey: String) {
            artist = artistRepository.getArtistStateFlow(publicKey)
        }
    }
