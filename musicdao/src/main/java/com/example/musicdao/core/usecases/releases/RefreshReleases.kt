package com.example.musicdao.core.usecases.releases

import android.os.Build
import androidx.annotation.RequiresApi
import com.example.musicdao.core.repositories.AlbumRepository

class RefreshReleases(private val albumRepository: AlbumRepository) {

    @RequiresApi(Build.VERSION_CODES.O)
    suspend operator fun invoke() {
        albumRepository.refreshCache()
    }
}
