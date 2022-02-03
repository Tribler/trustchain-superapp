package com.example.musicdao.ui.home

import androidx.lifecycle.*
import com.example.musicdao.AppContainer
import com.example.musicdao.repositories.ReleaseBlock
import com.example.musicdao.repositories.ReleaseRepository
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import java.io.File

class HomeScreenViewModel(
    releaseRepository: ReleaseRepository,
) : ViewModel() {
    fun getCover(it: ReleaseBlock): File? {
        return null
    }

    private val _releases: MutableLiveData<List<ReleaseBlock>> = MutableLiveData()
    val releases: LiveData<List<ReleaseBlock>> = _releases

    init {
        viewModelScope.launch {
            releaseRepository.getReleaseBlocks().collect {
                _releases.value = it
            }
        }
    }

    companion object {
        fun provideFactory(
            releaseRepository: ReleaseRepository = AppContainer.releaseRepository,
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return HomeScreenViewModel(releaseRepository) as T
            }
        }
    }

}
