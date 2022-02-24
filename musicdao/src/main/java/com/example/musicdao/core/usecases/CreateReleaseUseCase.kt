package com.example.musicdao.core.usecases

import android.content.Context
import android.net.Uri
import android.os.Build
import androidx.annotation.RequiresApi
import com.example.musicdao.core.ipv8.MusicCommunity
import com.example.musicdao.core.repositories.AlbumRepository
import com.example.musicdao.core.torrent.TorrentCache
import com.example.musicdao.core.util.MyResult
import javax.inject.Inject
import kotlin.io.path.name

class CreateReleaseUseCase @Inject constructor(
    private val albumRepository: AlbumRepository,
    private val torrentCache: TorrentCache,
    private val musicCommunity: MusicCommunity
) {

    @RequiresApi(Build.VERSION_CODES.O)
    operator fun invoke(
        artist: String,
        title: String,
        releaseDate: String,
        uris: List<Uri>,
        context: Context
    ): Boolean {
        // Copy the files into the cache with the appropriate folder i.e. simulating the download process
        val tempFolder = torrentCache.copyToTempFolder(context, uris)
        val cacheFolder = torrentCache.copyIntoCache(tempFolder.toPath())

        when (cacheFolder) {
            is MyResult.Failure -> return false
            is MyResult.Success -> {
                val infoHash = cacheFolder.value.parent.name

                val result = albumRepository.create(
                    releaseId = infoHash,
                    magnet = "magnet:?xt=urn:btih:$infoHash",
                    title = title,
                    artist = artist,
                    releaseDate = releaseDate,
                )

                if (result) {
                    torrentCache.seedStrategy()
                }

                return result
            }
        }
    }
}
