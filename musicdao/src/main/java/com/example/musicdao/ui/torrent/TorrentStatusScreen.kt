package com.example.musicdao.ui.torrent

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.Divider
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.ListItem
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.frostwire.jlibtorrent.TorrentHandle

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun TorrentStatusScreen(torrentHandle: TorrentHandle) {

    Column {
        ListItem(text = { Text("ID") },
            secondaryText = { Text(torrentHandle.status().name().toString()) })
        ListItem(text = { Text("Magnet Link") },
            secondaryText = { SelectionContainer { Text(torrentHandle.makeMagnetUri()) } })
        Divider()
        ListItem(text = { Text("Finished Downloading") },
            secondaryText = { Text(torrentHandle.status().isFinished.toString()) })
        ListItem(text = { Text("Pieces") },
            secondaryText = {
                Text(
                    "${torrentHandle.status().numPieces()}/${
                        torrentHandle.status().pieces().count()
                    }"
                )
            })
        ListItem(text = { Text("Files") },
            secondaryText = {
                Text("${torrentHandle.torrentFile().files()}}")
            })
        Divider()
        ListItem(text = { Text("Seeding") },
            secondaryText = { Text(torrentHandle.status().isSeeding.toString()) })

        Row {
            ListItem(
                text = { Text("Peers") },
                secondaryText = { Text(torrentHandle.status().numPeers().toString()) },
                modifier = Modifier.weight(1f)
            )
            ListItem(
                text = { Text("Seeders") },
                secondaryText = { Text(torrentHandle.status().numSeeds().toString()) },
                modifier = Modifier.weight(1f)
            )
        }
        Row {
            ListItem(
                text = { Text("Uploaded Bytes") },
                secondaryText = { Text(torrentHandle.status().allTimeUpload().toString()) },
                modifier = Modifier.weight(1f)
            )
            ListItem(
                text = { Text("Downloaded Bytes") },
                secondaryText = { Text(torrentHandle.status().allTimeDownload().toString()) },
                modifier = Modifier.weight(1f)
            )
        }
        Divider()
    }
}
