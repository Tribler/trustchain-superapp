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
    ): Boolean {
        // Copy the files into the cache with the appropiate folder
        // i.e. simulating the download process
        val tempFolder = torrentCache.copyToTempFolder(context, uris)
        val cacheFolder = torrentCache.copyIntoCache(tempFolder.toPath())

        when (cacheFolder) {
            is MyResult.Failure -> return false
            is MyResult.Success -> {
                val infoHash = cacheFolder.value.parent.name

                // Validate before publishing
                if (!releaseRepository.validateReleaseBlock(
                        releaseId = infoHash,
                        magnet = "magnet:?xt=urn:btih:$infoHash",
                        title = title,
                        artist = artist,
                        releaseDate = releaseDate,
                        publisher = publisher
                    )
                ) {
                    return false
                }

                releaseRepository.publishRelease(
                    releaseId = infoHash,
                    magnet = "magnet:?xt=urn:btih:$infoHash",
                    title = title,
                    artist = artist,
                    releaseDate = releaseDate,
                    publisher = publisher
                )
                torrentCache.seedStrategy()
                return true
            }
        }
    }
}
