package com.example.musicdao.ui.release

import android.net.Uri
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Menu
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.musicdao.repositories.ReleaseBlock
import com.example.musicdao.ui.components.ReleaseCover
import com.example.musicdao.ui.dateToShortString
import com.example.musicdao.ui.torrent.TorrentStatusScreen
import com.google.android.exoplayer2.SimpleExoPlayer
import com.google.android.exoplayer2.source.MediaSource
import com.google.android.exoplayer2.source.ProgressiveMediaSource
import com.google.android.exoplayer2.upstream.DataSource
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory
import java.io.File

@RequiresApi(Build.VERSION_CODES.O)
@ExperimentalMaterialApi
@Composable
fun ReleaseScreen(releaseId: String, exoPlayer: SimpleExoPlayer) {

    var state by remember { mutableStateOf(0) }
    val titles = listOf("RELEASE", "TORRENT")

    val viewModel: ReleaseScreenViewModel =
        viewModel(factory = ReleaseScreenViewModel.provideFactory(releaseId))

    val torrentStatus by viewModel.torrentHandleState.collectAsState()
    val saturatedRelease by viewModel.saturatedReleaseState.collectAsState()

    // Audio Player
    val context = LocalContext.current
    fun buildMediaSource(uri: Uri): MediaSource? {
        val dataSourceFactory: DataSource.Factory =
            DefaultDataSourceFactory(context, "musicdao-audioplayer")
        return ProgressiveMediaSource.Factory(dataSourceFactory)
            .createMediaSource(uri)
    }

    fun play(file: File) {
        val mediaSource = buildMediaSource(Uri.fromFile(file))
            ?: throw Error("Media source could not be instantiated")
        Log.d("MusicDAOTorrent", "Trying to play ${file}")
        exoPlayer.playWhenReady = true
        exoPlayer.seekTo(0, 0)
        exoPlayer.prepare(mediaSource, false, false)
    }

    val scrollState = rememberScrollState()
    Column(
        modifier = Modifier
            .verticalScroll(scrollState)
            .padding(bottom = 150.dp)
    ) {
        TabRow(selectedTabIndex = state) {
            titles.forEachIndexed { index, title ->
                Tab(
                    onClick = { state = index },
                    selected = (index == state),
                    text = { Text(title) })
            }
        }
        if (state == 0) {
            Column(
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .padding(top = 20.dp)
            ) {
                ReleaseCover(
                    file = saturatedRelease.cover,
                    modifier = Modifier
                        .height(200.dp)
                        .aspectRatio(1f)
                        .clip(RoundedCornerShape(10))
                        .background(Color.DarkGray)
                        .shadow(10.dp)
                        .align(Alignment.CenterHorizontally)
                )
            }
            Header(saturatedRelease.releaseBlock)
            if (saturatedRelease.files != null) {
                val files = saturatedRelease.files
                files?.map {
                    ListItem(text = { Text(it.name) },
                        secondaryText = { Text(it.artist) },
                        trailing = {
                            Icon(
                                imageVector = Icons.Filled.Menu,
                                contentDescription = null
                            )
                        },
                        modifier = Modifier.clickable { play(file = it.file) })
                }
            } else {
                if (torrentStatus != null) {
                    val downloadingTracks = torrentStatus?.downloadingTracks
                    downloadingTracks?.map {
                        Text(text = it.file.absolutePath)
                        ListItem(text = { Text(it.title) },
                            secondaryText = {
                                Column {
                                    Text(it.artist, modifier = Modifier.padding(bottom = 5.dp))
                                    LinearProgressIndicator(progress = it.progress.toFloat() / 100)
                                }
                            },
                            trailing = {
                                Icon(
                                    imageVector = Icons.Filled.Menu,
                                    contentDescription = null
                                )
                            },
                            modifier = Modifier.clickable {
//                                viewModel.setFilePriority(it)
                                play(it.file)
                            }
                        )
                    }
                }
            }


        }
        if (state == 1) {
            val current = torrentStatus
            if (current != null) {
                TorrentStatusScreen(current)
            } else {
                Text("Could not find torrent.")
            }
        }
    }
}

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun Header(releaseBlock: ReleaseBlock) {
    Column(modifier = Modifier.padding(20.dp)) {
        Text(
            releaseBlock.title,
            style = MaterialTheme.typography.h6.merge(SpanStyle(fontWeight = FontWeight.ExtraBold)),
            modifier = Modifier.padding(bottom = 5.dp)
        )
        Text(
            releaseBlock.artist,
            style = MaterialTheme.typography.body2.merge(SpanStyle(fontWeight = FontWeight.SemiBold)),
            modifier = Modifier.padding(bottom = 5.dp)
        )
        Text(
            "Album - ${dateToShortString(releaseBlock.releaseDate)}",
            style = MaterialTheme.typography.body2.merge(
                SpanStyle(fontWeight = FontWeight.SemiBold, color = Color.Gray)
            ), modifier = Modifier.padding(bottom = 10.dp)
        )
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(
                imageVector = Icons.Default.Favorite,
                contentDescription = null,
                modifier = Modifier.then(Modifier.padding(0.dp))
            )
            Button(onClick = {}) {
                Text("Donate", color = Color.White)
            }

        }
    }
}





