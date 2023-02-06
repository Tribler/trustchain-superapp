import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import nl.tudelft.trustchain.musicdao.ui.components.ReleaseCover
import nl.tudelft.trustchain.musicdao.ui.components.player.PlayerViewModel
import nl.tudelft.trustchain.musicdao.ui.navigation.Screen

@ExperimentalMaterialApi
@ExperimentalComposeUiApi
@Composable
fun MinimizedPlayer(
    playerViewModel: PlayerViewModel,
    navController: NavController,
    modifier: Modifier = Modifier
) {
    val coverFile by playerViewModel.coverFile.collectAsState(null)
    val track by playerViewModel.playingTrack.collectAsState(null)

    DisposableEffect(
        key1 = Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = modifier
                .height(60.dp)
                .fillMaxWidth()
                .background(MaterialTheme.colors.secondary)
        ) {

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .padding(10.dp)
                    .clickable { navController.navigate(Screen.FullPlayerScreen.route) }
                    .fillMaxWidth()
            ) {
                Row(modifier = Modifier.padding(end = 10.dp)) {
                    ReleaseCover(
                        file = coverFile,
                        modifier = Modifier
                            .fillMaxHeight()
                            .aspectRatio(1f)
                            .clip(RoundedCornerShape(5))
                    )
                    Column(
                        modifier = Modifier.padding(start = 10.dp),
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            track?.title?.uppercase() ?: "SONG NAME",
                            style = MaterialTheme.typography.button.merge(SpanStyle(fontWeight = FontWeight.Bold))
                        )
                        Text(
                            track?.artist?.uppercase() ?: "SONG ARTIST",
                            style = MaterialTheme.typography.button.merge(SpanStyle(fontWeight = FontWeight.Normal))
                        )
                    }
                }
            }
        },
        effect = {
            onDispose {
                playerViewModel.release()
            }
        }
    )
}
