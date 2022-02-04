package com.example.musicdao.ui.home

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.lifecycle.*
import com.example.musicdao.AppContainer
import com.example.musicdao.domain.usecases.GetReleaseUseCase
import com.example.musicdao.domain.usecases.SaturatedRelease
import com.example.musicdao.repositories.ReleaseRepository
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

@RequiresApi(Build.VERSION_CODES.O)
class HomeScreenViewModel(
    releaseRepository: ReleaseRepository,
    getReleaseUseCase: GetReleaseUseCase
) : ViewModel() {
    private val _releases: MutableLiveData<List<SaturatedRelease>> = MutableLiveData()
    val releases: LiveData<List<SaturatedRelease>> = _releases

    init {
        viewModelScope.launch {
            releaseRepository.getReleaseBlocks().collect { releaseBlock ->
                val releases = releaseBlock.map {
                    getReleaseUseCase.invoke(it.releaseId)
                }
                _releases.value = releases
            }
        }
    }

    companion object {
        fun provideFactory(
            releaseRepository: ReleaseRepository = AppContainer.releaseRepository,
            getReleaseUseCase: GetReleaseUseCase = AppContainer.getReleaseUseCase
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return HomeScreenViewModel(releaseRepository, getReleaseUseCase) as T
            }
        }
    }

}
