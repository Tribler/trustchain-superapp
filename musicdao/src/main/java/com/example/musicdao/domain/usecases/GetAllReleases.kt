package com.example.musicdao.domain.usecases

import android.os.Build
import androidx.annotation.RequiresApi
import com.example.musicdao.cache.CacheDatabase
import com.example.musicdao.model.Album

class GetAllReleases(private val database: CacheDatabase) {

    @RequiresApi(Build.VERSION_CODES.O)
    suspend operator fun invoke(): List<Album> {
        return database.dao.getAll().map { it.toAlbum() }
    }

}
