package com.example.musicdao.domain.usecases

import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import com.example.musicdao.model.Album
import com.example.musicdao.repositories.ReleaseRepository


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
