package com.example.musicdao.ui.debug

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.musicdao.AppContainer
import kotlinx.coroutines.delay

@Composable
fun Debug() {
    val sessionManager = AppContainer.sessionManager
    var dhtRunning by rememberSaveable { mutableStateOf(false) }
    var interfaces by rememberSaveable { mutableStateOf("") }
    var dhtNodes by rememberSaveable { mutableStateOf<Long>(0) }
    var uploadRate by rememberSaveable { mutableStateOf<Long>(0) }
    var downloadRate by rememberSaveable { mutableStateOf<Long>(0) }


    LaunchedEffect(Unit) {
        while (true) {
            dhtRunning = sessionManager.isDhtRunning
            interfaces = sessionManager.listenInterfaces()
            dhtNodes = sessionManager.dhtNodes()
            uploadRate = sessionManager.uploadRate()
            downloadRate = sessionManager.downloadRate()
            delay(2000)
        }
    }

    Column(modifier = Modifier.padding(20.dp)) {
        Text("Interfaces: ${dhtRunning}")
        Text("DHT Running: ${interfaces}")
        Text("DHT Peers: ${dhtNodes}")
        Text("Upload-rate: ${uploadRate}")
        Text("Download-rate: ${downloadRate}")
    }
}
