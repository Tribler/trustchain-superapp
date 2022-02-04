package com.example.musicdao.ui.search

import TorrentHandleStatus
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.musicdao.AppContainer
import com.example.musicdao.domain.usecases.torrents.GetAllActiveTorrentsUseCase
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

@RequiresApi(Build.VERSION_CODES.O)
class DebugScreenViewModel(
    private val getAllActiveTorrentsUseCase: GetAllActiveTorrentsUseCase
) : ViewModel() {

    private val _status: MutableStateFlow<List<Flow<TorrentHandleStatus>>> =
        MutableStateFlow(listOf())
    val status: StateFlow<List<Flow<TorrentHandleStatus>>> = _status

    init {
        viewModelScope.launch {
            getAllActiveTorrentsUseCase.invoke().stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000L),
                initialValue = listOf()
            ).collect {
                _status.value = it
            }
        }
    }

    companion object {
        fun provideFactory(
            getAllActiveTorrentsUseCase: GetAllActiveTorrentsUseCase = AppContainer.getAllActiveTorrentsUseCase
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return DebugScreenViewModel(getAllActiveTorrentsUseCase) as T
            }
        }
    }
}
