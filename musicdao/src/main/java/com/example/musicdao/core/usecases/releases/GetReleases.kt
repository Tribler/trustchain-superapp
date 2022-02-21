package com.example.musicdao.core.usecases.releases

import android.os.Build
import androidx.annotation.RequiresApi
import com.example.musicdao.core.database.CacheDatabase
import com.example.musicdao.core.model.Album
import javax.inject.Inject

class GetReleases @Inject constructor(private val database: CacheDatabase) {

    @RequiresApi(Build.VERSION_CODES.O)
    suspend operator fun invoke(): List<Album> {
        return database.dao.getAll().map { it.toAlbum() }
    }

}
