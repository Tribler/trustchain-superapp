package com.example.musicdao.domain.usecases

import android.os.Build
import androidx.annotation.RequiresApi
import com.example.musicdao.cache.CacheDatabase
import com.example.musicdao.model.Album

class GetRelease(private val database: CacheDatabase) {

    @RequiresApi(Build.VERSION_CODES.O)
    suspend operator fun invoke(id: String): Album {
        return database.dao.get(id).toAlbum()
    }

}
