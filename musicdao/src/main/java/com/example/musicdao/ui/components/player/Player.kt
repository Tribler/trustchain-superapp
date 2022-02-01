import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester.Companion.createRefs
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.google.android.exoplayer2.SimpleExoPlayer
import com.google.android.exoplayer2.ui.PlayerView

@ExperimentalComposeUiApi
@Composable
fun VideoPlayer(modifier: Modifier = Modifier, exoPlayer: SimpleExoPlayer) {
    val context = LocalContext.current

    Column(
        modifier = modifier
            .background(Color.Black)
    ) {
        val (title, videoPlayer) = createRefs()

        // video title
//        Text(
//            text = "Current Title",
//            color = Color.White,
//            modifier =
//            Modifier
//                .padding(start = 40.dp, top = 10.dp)
//                .fillMaxWidth()
//                .wrapContentHeight()
//        )

        // player view
        DisposableEffect(
            AndroidView(
                modifier = Modifier.padding(bottom = 20.dp),
                factory = {
                    PlayerView(context).apply {
                        player = exoPlayer
                        layoutParams = FrameLayout.LayoutParams(
                            ViewGroup.LayoutParams.WRAP_CONTENT,
                            200
                        )
                        controllerShowTimeoutMs = 0
                    }
                }
            )
        ) {
            onDispose {
                exoPlayer.release()
            }
        }
    }


}
