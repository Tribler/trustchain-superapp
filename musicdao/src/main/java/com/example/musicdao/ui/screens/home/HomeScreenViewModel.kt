package com.example.musicdao.ui.screens.home

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.musicdao.core.repositories.model.Album
import com.example.musicdao.core.repositories.AlbumRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import javax.inject.Inject

@RequiresApi(Build.VERSION_CODES.O)
@HiltViewModel
class HomeScreenViewModel @Inject constructor(
    private val albumRepository: AlbumRepository
) : ViewModel() {

    private val _releases: MutableLiveData<List<Album>> = MutableLiveData()
    var releases: LiveData<List<Album>> = _releases

    private val _isRefreshing: MutableLiveData<Boolean> = MutableLiveData()
    val isRefreshing: LiveData<Boolean> = _isRefreshing

    init {
        viewModelScope.launch {
            releases = albumRepository.getAlbumsFlow()
        }
    }

    fun refresh() {
        viewModelScope.launch {
            _isRefreshing.value = true
            delay(500)
            _releases.value = albumRepository.getAlbums()
            _isRefreshing.value = false
        }
    }
}
