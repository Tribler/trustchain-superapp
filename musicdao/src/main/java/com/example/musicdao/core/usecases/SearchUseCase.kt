package com.example.musicdao.core.usecases

import android.os.Build
import androidx.annotation.RequiresApi
import com.example.musicdao.core.model.Album
import com.example.musicdao.core.repositories.ReleaseRepository


class SearchUseCase(
    private val releaseRepository: ReleaseRepository,
    private val getSaturatedRelease: GetRelease
) {

    @RequiresApi(Build.VERSION_CODES.O)
    suspend operator fun invoke(searchText: String): List<Album> {
        val blocks = releaseRepository.searchReleaseBlocksLocal(searchText)
        return blocks.map { getSaturatedRelease.invoke(it.releaseId) }
    }

}
