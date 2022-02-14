package com.example.musicdao.core.usecases

import android.os.Build
import androidx.annotation.RequiresApi
import com.example.musicdao.core.cache.CacheDatabase
import com.example.musicdao.core.model.Album

class GetAllReleases(private val database: CacheDatabase) {

    @RequiresApi(Build.VERSION_CODES.O)
    suspend operator fun invoke(): List<Album> {
        return database.dao.getAll().map { it.toAlbum() }
    }

}
