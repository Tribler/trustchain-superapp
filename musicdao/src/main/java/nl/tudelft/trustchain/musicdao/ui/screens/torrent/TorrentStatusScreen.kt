package nl.tudelft.trustchain.musicdao.ui.screens.torrent

import nl.tudelft.trustchain.musicdao.core.torrent.status.TorrentStatus
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
fun TorrentStatusScreen(torrent: TorrentStatus) {

    Column {
        ListItem(text = { Text("Info Hash") }, secondaryText = { Text(torrent.infoHash) })
        ListItem(
            text = { Text("Magnet Link") },
            secondaryText = { SelectionContainer { Text(torrent.magnet) } },
            modifier = Modifier.padding(bottom = 10.dp)
        )
        Divider()
        ListItem(
            text = { Text("Finished Downloading") },
            secondaryText = { Text(torrent.finishedDownloading) }
        )
        ListItem(text = { Text("Pieces") }, secondaryText = { Text(torrent.pieces) })
        ListItem(text = { Text("Files") }, secondaryText = { Text("${torrent.files}") })
        Divider()
        ListItem(
            text = { Text("Seeding") },
            secondaryText = { Text(torrent.seeding) }
        )

        Row {
            ListItem(
                text = { Text("Peers") },
                secondaryText = { Text(torrent.peers) },
                modifier = Modifier.weight(1f)
            )
            ListItem(
                text = { Text("Seeders") },
                secondaryText = { Text(torrent.seeders) },
                modifier = Modifier.weight(1f)
            )
        }
        Row {
            ListItem(
                text = { Text("Uploaded Bytes") },
                secondaryText = { Text(torrent.uploadedBytes) },
                modifier = Modifier.weight(1f)
            )
            ListItem(
                text = { Text("Downloaded Bytes") },
                secondaryText = { Text(torrent.downloadedBytes) },
                modifier = Modifier.weight(1f)
            )
        }
        Divider()
    }
}
