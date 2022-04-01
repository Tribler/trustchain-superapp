package com.example.musicdao.ui.screens.donate

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.lifecycle.ViewModel
import com.example.musicdao.core.repositories.ArtistRepository
import com.example.musicdao.core.repositories.model.Artist
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import javax.inject.Inject

@RequiresApi(Build.VERSION_CODES.O)
@HiltViewModel
class DonateScreenViewModel @Inject constructor(val artistRepository: ArtistRepository) : ViewModel() {

    val artist: MutableStateFlow<Artist?> = MutableStateFlow(null)

    @RequiresApi(Build.VERSION_CODES.O)
    suspend fun setArtist(publicKey: String) {
        artist.value = artistRepository.getArtist(publicKey)
    }
}
