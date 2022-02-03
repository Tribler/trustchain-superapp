package com.example.musicdao.domain.usecases

import TorrentCache
import android.content.Context
import android.net.Uri
import android.os.Build
import androidx.annotation.RequiresApi
import com.example.musicdao.repositories.ReleaseRepository
import com.example.musicdao.util.MyResult
import kotlin.io.path.name

class CreateReleaseUseCase(
    private val releaseRepository: ReleaseRepository,
    private val torrentCache: TorrentCache
) {

    @RequiresApi(Build.VERSION_CODES.O)
    operator fun invoke(
        artist: String,
        title: String,
        releaseDate: String,
        publisher: String,
        uris: List<Uri>,
        context: Context
    ) {
        val tempFolder = torrentCache.copyToTempFolder(context, uris)
        val cacheFolder = torrentCache.copyIntoCache(tempFolder.toPath())

        when (cacheFolder) {
            is MyResult.Failure -> TODO()
            is MyResult.Success -> {
                releaseRepository.publishRelease(
                    cacheFolder.value.parent.name,
                    title,
                    artists = artist,
                    releaseDate,
                    cacheFolder.value.parent.name
                )
                torrentCache.seedStrategy()
            }
        }
    }
}
