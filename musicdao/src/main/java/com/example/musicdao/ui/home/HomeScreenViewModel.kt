package com.example.musicdao.ui.home

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.lifecycle.*
import com.example.musicdao.AppContainer
import com.example.musicdao.domain.usecases.GetAllReleases
import com.example.musicdao.model.Album
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@RequiresApi(Build.VERSION_CODES.O)
class HomeScreenViewModel(
    private val getAllReleases: GetAllReleases
) : ViewModel() {

    private val _releases: MutableLiveData<List<Album>> = MutableLiveData()
    val releases: LiveData<List<Album>> = _releases

    private val _isRefreshing: MutableLiveData<Boolean> = MutableLiveData()
    val isRefreshing: LiveData<Boolean> = _isRefreshing

    init {
        viewModelScope.launch {
            _releases.value = getAllReleases.invoke()
        }
    }

    fun refresh() {
        viewModelScope.launch {
            _isRefreshing.value = true
            delay(500)
            _releases.value = getAllReleases.invoke()
            _isRefreshing.value = false
        }
    }

    companion object {
        fun provideFactory(
            getAllReleases: GetAllReleases = AppContainer.getAllReleases
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return HomeScreenViewModel(
                    getAllReleases
                ) as T
            }
        }
    }

}
