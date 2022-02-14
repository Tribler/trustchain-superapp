package com.example.musicdao.ui.home

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.lifecycle.*
import com.example.musicdao.AppContainer
import com.example.musicdao.core.usecases.releases.GetReleases
import com.example.musicdao.core.model.Album
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@RequiresApi(Build.VERSION_CODES.O)
class HomeScreenViewModel(
    private val getReleases: GetReleases
) : ViewModel() {

    private val _releases: MutableLiveData<List<Album>> = MutableLiveData()
    val releases: LiveData<List<Album>> = _releases

    private val _isRefreshing: MutableLiveData<Boolean> = MutableLiveData()
    val isRefreshing: LiveData<Boolean> = _isRefreshing

    init {
        viewModelScope.launch {
            _releases.value = getReleases.invoke()
        }
    }

    fun refresh() {
        viewModelScope.launch {
            _isRefreshing.value = true
            delay(500)
            _releases.value = getReleases.invoke()
            _isRefreshing.value = false
        }
    }

    companion object {
        fun provideFactory(
            getReleases: GetReleases = AppContainer.getReleases
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return HomeScreenViewModel(
                    getReleases
                ) as T
            }
        }
    }

}
