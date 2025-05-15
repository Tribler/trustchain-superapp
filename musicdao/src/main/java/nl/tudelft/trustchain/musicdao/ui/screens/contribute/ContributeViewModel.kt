package nl.tudelft.trustchain.musicdao.ui.screens.contribute

import dagger.hilt.android.lifecycle.HiltViewModel
import nl.tudelft.trustchain.musicdao.core.repositories.ArtistRepository
import javax.inject.Inject
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import nl.tudelft.ipv8.attestation.trustchain.TrustChainBlock
import nl.tudelft.trustchain.musicdao.core.contribute.Contribution
import nl.tudelft.trustchain.musicdao.core.repositories.model.Artist
import nl.tudelft.trustchain.musicdao.ui.screens.wallet.BitcoinWalletViewModel
import java.util.*;

@HiltViewModel
class ContributeViewModel
    @Inject
    constructor(
        private val artistRepository: ArtistRepository
    ) : ViewModel() {

    private val _contributions: MutableStateFlow<List<Contribution>> = MutableStateFlow(listOf())
    val contributions: StateFlow<List<Contribution>> = _contributions

    private val contributionPool = ContributionPool

    // Add contributions to the shared pool
    fun contribute(amount: Double): Boolean {
        val artists = artistRepository.getArtists()

        if (artists.isNotEmpty()) {
            val share = amount / artists.size
            artists.forEach { artist ->
                contributionPool.addContribution(artist, share)
            }
            val contribution = Contribution(amount, artists)
            _contributions.value = _contributions.value + contribution
            return true
        }

        return false
    }

//    // make a method that calls distributePooledContributions from the shared pool
//    fun distributeContributions() {
//        CoroutineScope(Dispatchers.IO).launch {
//            contributionPool.distributePooledContributions(bitcoinWalletViewModel)
//        }
//    }

    fun clearContributions() {
        _contributions.value = listOf()
    }

    // old one before shared pool
//    // return true if all donations were successful and false if any failed
//    suspend fun contribute(amount: Long): Boolean {
//
//        // TODO: replace this with only the artists I have listened to
//        val artists = artistRepository.getArtists()
//
//        if (artists.isNotEmpty()) {
//            val share = amount / artists.size
//            artists.forEach { artist ->
//                val succ = bitcoinWalletViewModel.donate(artist.publicKey, share.toString())
//
//                if (!succ) {
//                    return false
//                }
//            }
//            val contribution = Contribution(amount, artists)
//            _contributions.value = _contributions.value + contribution
//        }
//
//
//        return true
//    }
}
