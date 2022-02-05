package com.example.musicdao.ui.components.player

import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.musicdao.ui.components.ReleaseCover
import com.google.android.exoplayer2.ui.PlayerView

@Composable
fun FullPlayerScreen(playerViewModel: PlayerViewModel) {
    val context = LocalContext.current
    val coverFile by playerViewModel.coverFile.collectAsState(null)
    val track by playerViewModel.playingTrack.collectAsState(null)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp)
    ) {
        Column(modifier = Modifier.padding(bottom = 10.dp)) {
            ReleaseCover(
                file = coverFile,
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
                    .clip(RoundedCornerShape(5))
                    .shadow(10.dp)
                    .align(Alignment.CenterHorizontally)
            )
            Column(modifier = Modifier.padding(top = 10.dp)) {
                Text(
                    track?.name ?: "",
                    style = MaterialTheme.typography.h6.merge(SpanStyle(fontWeight = FontWeight.ExtraBold)),
                    modifier = Modifier.padding(bottom = 5.dp)
                )
                Text(
                    track?.artist ?: "",
                    style = MaterialTheme.typography.body2.merge(SpanStyle(fontWeight = FontWeight.SemiBold)),
                    modifier = Modifier.padding(bottom = 5.dp)
                )
            }
        }

        Column(modifier = Modifier.background(Color.Black)) {
            DisposableEffect(
                AndroidView(
                    modifier = Modifier.padding(bottom = 20.dp),
                    factory = {
                        PlayerView(context).apply {
                            player = playerViewModel.exoPlayer
                            layoutParams = FrameLayout.LayoutParams(
                                ViewGroup.LayoutParams.WRAP_CONTENT,
                                200
                            )
                            controllerShowTimeoutMs = -1
                        }
                    }
                )
            ) {
                onDispose {}
            }
        }
    }
}
