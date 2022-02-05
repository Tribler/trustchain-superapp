package com.example.musicdao.ui.torrent

import TorrentHandleStatus
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.Divider
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.ListItem
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp


@OptIn(ExperimentalMaterialApi::class)
@Composable
fun TorrentStatusScreen(torrentHandle: TorrentHandleStatus) {

    Column {
        ListItem(text = { Text("Info Hash") }, secondaryText = { Text(torrentHandle.infoHash) })
        ListItem(
            text = { Text("Magnet Link") },
            secondaryText = { SelectionContainer { Text(torrentHandle.magnet) } },
            modifier = Modifier.padding(bottom = 10.dp)
        )
        Divider()
        ListItem(
            text = { Text("Finished Downloading") },
            secondaryText = { Text(torrentHandle.finishedDownloading) })
        ListItem(text = { Text("Pieces") }, secondaryText = { Text(torrentHandle.pieces) })
        ListItem(text = { Text("Files") }, secondaryText = { Text("${torrentHandle.files}") })
        Divider()
        ListItem(text = { Text("Seeding") },
            secondaryText = { Text(torrentHandle.seeding) })

        Row {
            ListItem(
                text = { Text("Peers") },
                secondaryText = { Text(torrentHandle.peers) },
                modifier = Modifier.weight(1f)
            )
            ListItem(
                text = { Text("Seeders") },
                secondaryText = { Text(torrentHandle.seeders) },
                modifier = Modifier.weight(1f)
            )
        }
        Row {
            ListItem(
                text = { Text("Uploaded Bytes") },
                secondaryText = { Text(torrentHandle.uploadedBytes) },
                modifier = Modifier.weight(1f)
            )
            ListItem(
                text = { Text("Downloaded Bytes") },
                secondaryText = { Text(torrentHandle.downloadedBytes) },
                modifier = Modifier.weight(1f)
            )
        }
        Divider()
    }
}
