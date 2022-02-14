package com.example.musicdao.core.usecases

import android.os.Build
import androidx.annotation.RequiresApi
import com.example.musicdao.core.cache.CacheDatabase
import com.example.musicdao.core.model.Album

class GetRelease(private val database: CacheDatabase) {

    @RequiresApi(Build.VERSION_CODES.O)
    suspend operator fun invoke(id: String): Album {
        return database.dao.get(id).toAlbum()
    }

}
