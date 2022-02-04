package com.example.musicdao.domain.usecases

import TorrentCache
import android.os.Build
import androidx.annotation.RequiresApi
import com.example.musicdao.repositories.ReleaseBlock
import com.example.musicdao.repositories.ReleaseRepository
import com.example.musicdao.util.Util
import com.mpatric.mp3agic.Mp3File
import java.io.File

data class SaturatedRelease(
    val releaseBlock: ReleaseBlock,
    val files: List<Track>?,
    val cover: File?
)

data class Track(
    val file: File,
    val name: String,
    val artist: String
)

class GetReleaseUseCase(
    private val releaseRepository: ReleaseRepository,
    private val torrentCache: TorrentCache
) {

    @RequiresApi(Build.VERSION_CODES.O)
    operator fun invoke(id: String): SaturatedRelease {
        val releaseBlock = releaseRepository.getReleaseBlock(id)

        val files = torrentCache.getFiles(id)
        val tracks = files?.mapNotNull {
            try {
                val mp3 = Mp3File(it)
                Track(
                    file = it,
                    name = Util.getTitle(mp3) ?: (Util.checkAndSanitizeTrackNames(it.name)
                        ?: it.name),
                    artist = releaseBlock.artist
                )
            } catch (e: Exception) {
                null
            }
        }

        var cover: File? = null
        if (files != null && files.isNotEmpty()) {
            cover = Util.findCoverArt(files.get(0).parentFile.parentFile)
        }
        return SaturatedRelease(
            releaseBlock,
            tracks,
            cover
        )
    }

}
