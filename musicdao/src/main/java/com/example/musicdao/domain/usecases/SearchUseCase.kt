package com.example.musicdao.domain.usecases

import android.os.Build
import androidx.annotation.RequiresApi
import com.example.musicdao.repositories.ReleaseRepository


class SearchUseCase(
    private val releaseRepository: ReleaseRepository,
    private val getSaturatedRelease: GetReleaseUseCase
) {

    @RequiresApi(Build.VERSION_CODES.O)
    operator fun invoke(searchText: String): List<SaturatedRelease> {
        val blocks = releaseRepository.searchReleaseBlocksLocal(searchText)
        return blocks.map { getSaturatedRelease.invoke(it.releaseId) }
    }

}
