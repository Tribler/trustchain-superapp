package com.example.musicdao.domain.usecases

import com.example.musicdao.repositories.ReleaseBlock
import com.example.musicdao.repositories.ReleaseRepository
import com.example.musicdao.repositories.TorrentRepository
import com.example.musicdao.util.Util
import java.io.File

data class SaturatedRelease(
    val releaseBlock: ReleaseBlock,
    val files: List<File>?,
    val cover: File?
)

class GetReleaseUseCase(
    private val releaseRepository: ReleaseRepository,
    private val torrentRepository: TorrentRepository
) {

    operator fun invoke(id: String): SaturatedRelease {
        val releaseBlock = releaseRepository.getReleaseBlock(id)

        val files = torrentRepository.getFiles(id)
        var cover: File? = null
        if (files.isNotEmpty()) {
            cover = Util.findCoverArt(files.get(0).parentFile)
        }
        return SaturatedRelease(
            releaseBlock,
            files,
            cover
        )
    }

}
