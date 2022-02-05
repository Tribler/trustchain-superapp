package com.example.musicdao.ui.home

import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.lifecycle.*
import com.example.musicdao.AppContainer
import com.example.musicdao.domain.usecases.GetReleaseUseCase
import com.example.musicdao.domain.usecases.SaturatedRelease
import com.example.musicdao.repositories.ReleaseRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

@RequiresApi(Build.VERSION_CODES.O)
class HomeScreenViewModel(
    val releaseRepository: ReleaseRepository,
    getReleaseUseCase: GetReleaseUseCase
) : ViewModel() {

    private val _releases: MutableLiveData<List<SaturatedRelease>> = MutableLiveData()
    val releases: LiveData<List<SaturatedRelease>> = _releases

    private val _isRefreshing: MutableLiveData<Boolean> = MutableLiveData()
    val isRefreshing: LiveData<Boolean> = _isRefreshing

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

    fun refresh() {
        viewModelScope.launch {
            _isRefreshing.value = true
            delay(500)
            releaseRepository.refreshReleases()
            _isRefreshing.value = false
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
