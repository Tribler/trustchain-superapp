package nl.tudelft.trustchain.musicdao.ui.screens.profile

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
import nl.tudelft.trustchain.musicdao.core.model.AccountType
import nl.tudelft.trustchain.musicdao.core.services.UserTierService
import java.time.Instant
import javax.inject.Inject

class ProfileScreenViewModel
    @AssistedInject
    constructor(
        @Assisted private val publicKey: String,
        private val artistRepository: ArtistRepository,
        private val userTierService: UserTierService
    ) : ViewModel() {
        private val _profile: MutableStateFlow<Artist?> = MutableStateFlow(null)
        var profile: StateFlow<Artist?> = _profile

        private val _releases: MutableStateFlow<List<Album>> = MutableStateFlow(listOf())
        val releases: StateFlow<List<Album>> = _releases

        private val _accountType = MutableStateFlow(AccountType.BASIC)
        val accountType: StateFlow<AccountType> = _accountType

        private val _validUntil = MutableStateFlow<Instant?>(null)
        val validUntil: StateFlow<Instant?> = _validUntil

        init {
            viewModelScope.launch {
                profile = artistRepository.getArtistStateFlow(publicKey = publicKey)
                _releases.value = artistRepository.getArtistReleases(publicKey = publicKey)
            }
            loadTierStatus()
        }

        private fun loadTierStatus() {
            viewModelScope.launch {
                // TODO: Implement loading tier status from TrustChain
                // For now, default to BASIC
                _accountType.value = AccountType.BASIC
                _validUntil.value = null
            }
        }

        fun upgradeToPro(months: Int = 1) {
            viewModelScope.launch {
                val success = userTierService.upgradeToPro(
                    userId = "TODO: Get user ID", // TODO: Get actual user ID
                    durationMonths = months
                )
                
                if (success) {
                    _accountType.value = AccountType.PRO
                    // Calculate validUntil based on months
                    _validUntil.value = Instant.now().plusSeconds(months * 30L * 24L * 60L * 60L)
                }
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
            ): ViewModelProvider.Factory =
                object : ViewModelProvider.Factory {
                    override fun <T : ViewModel> create(modelClass: Class<T>): T {
                        @Suppress("UNCHECKED_CAST")
                        return assistedFactory.create(publicKey) as T
                    }
                }
        }
    }
