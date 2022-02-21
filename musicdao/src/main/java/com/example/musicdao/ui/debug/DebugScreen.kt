package com.example.musicdao.ui.debug

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Person
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.musicdao.core.torrent.api.TorrentHandleStatus
import com.example.musicdao.ui.components.EmptyState
import com.example.musicdao.ui.search.DebugScreenViewModel

@ExperimentalMaterialApi
@Composable
fun Debug(debugScreenViewModel: DebugScreenViewModel) {

    val torrentHandleStatus by debugScreenViewModel.status.collectAsState(listOf())
    val sessionStatus by debugScreenViewModel.sessionStatus.collectAsState()

    Column {
        Column(modifier = Modifier.padding(20.dp)) {
            Text("Interface: ${sessionStatus?.interfaces}")
            Text("DHT Peers: ${sessionStatus?.dhtNodes}")
            Text("Upload-rate: ${sessionStatus?.uploadRate}")
            Text("Download-rate: ${sessionStatus?.downloadRate}")
        }
        Divider()
        LazyColumn {
            items(torrentHandleStatus) {
                val item = it.collectAsState(null)
                val itemValue = item.value
                if (itemValue != null) {
                    TorrentStatusListItem(itemValue)
                    Divider()
                }
            }
        }
        if (torrentHandleStatus.isEmpty()) {
            EmptyState(
                firstLine = "No torrents active",
                secondLine = "Currently there are no torrents seeding or downloading",
            )
        }
        Column(modifier = Modifier.height(height = 200.dp)) {}
    }
}

@ExperimentalMaterialApi
@Composable
fun TorrentStatusListItem(torrentHandleStatus: TorrentHandleStatus) {
    ListItem(
        text = { Text(torrentHandleStatus.infoHash) },
        secondaryText = {
            Column {
                Text(torrentHandleStatus.magnet)
                Column(modifier = Modifier.padding(vertical = 10.dp)) {
                    if (torrentHandleStatus.seeding == "true") {
                        LinearProgressIndicator(1.0f)
                    } else {
                        LinearProgressIndicator()
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(imageVector = Icons.Default.Person, contentDescription = null)
                    Text(torrentHandleStatus.peers)
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(imageVector = Icons.Default.KeyboardArrowUp, contentDescription = null)
                    Text(torrentHandleStatus.uploadedBytes)
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.KeyboardArrowDown,
                        contentDescription = null
                    )
                    Text(torrentHandleStatus.downloadedBytes)
                }
            }
        },
        overlineText = {
            if (torrentHandleStatus.seeding == "true") {
                Text("Seeding")
            } else {
                Text("Downloading")
            }
        },
        icon = {
            // TODO: turn this into boolean
            if (torrentHandleStatus.seeding == "true") {
                Icon(imageVector = Icons.Default.KeyboardArrowUp, contentDescription = null)
            } else {
                Icon(imageVector = Icons.Default.KeyboardArrowDown, contentDescription = null)
            }
        },
        modifier = Modifier
            .clickable {}
            .padding(bottom = 20.dp)
    )
}
