package com.example.musicdao.domain.usecases

import TorrentCache
import android.os.Build
import androidx.annotation.RequiresApi
import com.example.musicdao.repositories.ReleaseBlock
import com.example.musicdao.repositories.ReleaseRepository
import com.example.musicdao.util.Util
import java.io.File

data class SaturatedRelease(
    val releaseBlock: ReleaseBlock,
    val files: List<File>?,
    val cover: File?
)

class GetReleaseUseCase(
    private val releaseRepository: ReleaseRepository,
    private val torrentCache: TorrentCache
) {

    @RequiresApi(Build.VERSION_CODES.O)
    operator fun invoke(id: String): SaturatedRelease {
        val releaseBlock = releaseRepository.getReleaseBlock(id)

        val files = torrentCache.getFiles(id)
        var cover: File? = null
        if (files != null && files.isNotEmpty()) {
            cover = Util.findCoverArt(files.get(0).parentFile.parentFile)
        }
        return SaturatedRelease(
            releaseBlock,
            files,
            cover
        )
    }

}
