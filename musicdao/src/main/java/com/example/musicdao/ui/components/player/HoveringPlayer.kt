package com.example.musicdao.ui

import VideoPlayer
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.google.android.exoplayer2.SimpleExoPlayer

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun HoveringPlayer(paddingValues: PaddingValues, exoPlayer: SimpleExoPlayer, modifier: Modifier = Modifier) {
    Row(modifier = modifier
        .padding(7.dp)
        .clip(RoundedCornerShape(10)),
        content = { VideoPlayer(exoPlayer = exoPlayer) })
}
