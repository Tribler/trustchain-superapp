package nl.tudelft.trustchain.musicdao.ui.screens.debug

import android.annotation.SuppressLint
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
import nl.tudelft.trustchain.musicdao.core.torrent.status.TorrentStatus
import nl.tudelft.trustchain.musicdao.ui.components.EmptyState

@ExperimentalMaterialApi
@Composable
@SuppressLint("NewApi")
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
                TorrentStatusListItem(it)
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

@ExperimentalMaterialApi
@Composable
fun TorrentStatusListItem(torrentStatus: TorrentStatus) {
    ListItem(
        text = { Text(torrentStatus.infoHash) },
        secondaryText = {
            Column {
                Text(torrentStatus.magnet)
                Column(modifier = Modifier.padding(vertical = 10.dp)) {
                    if (torrentStatus.seeding == "true") {
                        LinearProgressIndicator(1.0f)
                    } else {
                        LinearProgressIndicator()
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(imageVector = Icons.Default.Person, contentDescription = null)
                    Text(torrentStatus.peers)
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(imageVector = Icons.Default.KeyboardArrowUp, contentDescription = null)
                    Text(torrentStatus.uploadedBytes)
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.KeyboardArrowDown,
                        contentDescription = null
                    )
                    Text(torrentStatus.downloadedBytes)
                }
            }
        },
        overlineText = {
            if (torrentStatus.seeding == "true") {
                Text("Seeding")
            } else {
                Text("Downloading")
            }
        },
        icon = {
            // TODO: turn this into boolean
            if (torrentStatus.seeding == "true") {
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
