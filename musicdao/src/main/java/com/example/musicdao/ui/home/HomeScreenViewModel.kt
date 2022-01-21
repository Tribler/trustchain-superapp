package com.example.musicdao.ui.home

import androidx.lifecycle.*
import com.example.musicdao.repositories.ReleaseBlock
import com.example.musicdao.repositories.ReleaseRepository
import com.example.musicdao.repositories.TorrentRepository
import com.example.musicdao.util.Util
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import java.io.File

class HomeScreenViewModel(
    releaseRepository: ReleaseRepository,
    val torrentRepository: TorrentRepository
) : ViewModel() {

    private val _releases: MutableLiveData<List<ReleaseBlock>> = MutableLiveData()
    val releases: LiveData<List<ReleaseBlock>> = _releases

    init {
        viewModelScope.launch {
            releaseRepository.getReleaseBlocks().collect {
                _releases.value = it
            }
        }
    }

    fun getCover(releaseBlock: ReleaseBlock): File? {
        val torrentInfoName = releaseBlock.torrentInfoName
        val isDownloaded = torrentRepository.isDownloaded(torrentInfoName)
        val files = torrentRepository.getFiles(torrentInfoName)
        if (files.size != 0) {
            return Util.findCoverArt(files.get(0).parentFile)
        } else {
            return null
        }
    }

    companion object {
        fun provideFactory(
            releaseRepository: ReleaseRepository,
            torrentRepository: TorrentRepository
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return HomeScreenViewModel(releaseRepository, torrentRepository) as T
            }
        }
    }

}
