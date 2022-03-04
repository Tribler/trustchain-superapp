package com.example.musicdao.ui.screens.search

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.musicdao.core.torrent.api.TorrentHandleStatus
import com.example.musicdao.core.usecases.torrents.GetAllActiveTorrentsUseCase
import com.example.musicdao.core.usecases.torrents.GetSessionManagerStatus
import com.example.musicdao.core.usecases.torrents.SessionManagerStatus
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import javax.inject.Inject

@RequiresApi(Build.VERSION_CODES.O)
@HiltViewModel
class DebugScreenViewModel @Inject constructor(
    private val getAllActiveTorrentsUseCase: GetAllActiveTorrentsUseCase,
    private val getGetSessionManagerStatus: GetSessionManagerStatus
) : ViewModel() {

    private val _status: MutableStateFlow<List<Flow<TorrentHandleStatus>>> =
        MutableStateFlow(listOf())
    val status: StateFlow<List<Flow<TorrentHandleStatus>>> = _status

    private val _sessionStatus: MutableStateFlow<SessionManagerStatus?> = MutableStateFlow(null)
    val sessionStatus: StateFlow<SessionManagerStatus?> = _sessionStatus

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

        viewModelScope.launch {
            while (isActive) {
                _sessionStatus.value = getGetSessionManagerStatus.invoke()
                delay(2000)
            }
        }
    }
}
