package nl.tudelft.trustchain.musicdao.ui.screens.home

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import nl.tudelft.trustchain.musicdao.core.ipv8.MusicCommunity
import nl.tudelft.trustchain.musicdao.core.repositories.AlbumRepository
import nl.tudelft.trustchain.musicdao.core.repositories.ReleaseRepository
import nl.tudelft.trustchain.musicdao.core.repositories.model.Album
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class HomeScreenViewModel
    @Inject
    constructor(
        private val albumRepository: AlbumRepository,
        private val releaseRepository: ReleaseRepository,
        private val musicCommunity: MusicCommunity
    ) : ViewModel() {
        private val _releases: MutableLiveData<List<Album>> = MutableLiveData()
        var releases: LiveData<List<Album>> = _releases

        @Suppress("ktlint:standard:property-naming")
        private val _peerAmount: MutableLiveData<Int> = MutableLiveData()

        init {
            viewModelScope.launch {
                // Get the user's public key from MusicCommunity
                val userPublicKey = musicCommunity.publicKeyHex()

                // Observe the albums flow
                albumRepository.getAlbumsFlow(userPublicKey).observeForever { albums ->
                    // For each album, fetch the magnet link in the background
                    albums.forEach { album ->
                        viewModelScope.launch {
                            val magnetLink = releaseRepository.getFullRelease(album.id, userPublicKey)
                            if (magnetLink != null) {
                                // Update the album with the magnet link
                                val updatedAlbums = _releases.value?.map {
                                    if (it.id == album.id) {
                                        it.copy(magnet = magnetLink)
                                    } else {
                                        it
                                    }
                                } ?: albums
                                _releases.value = updatedAlbums
                            }
                        }
                    }
                }

                _peerAmount.value = musicCommunity.getPeers().size
            }
        }
    }
