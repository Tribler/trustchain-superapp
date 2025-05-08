package nl.tudelft.trustchain.musicdao.ui.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import nl.tudelft.trustchain.musicdao.core.cache.CacheDatabase
import nl.tudelft.trustchain.musicdao.core.cache.entities.AlbumEntity
import nl.tudelft.trustchain.musicdao.core.repositories.AlbumRepository
import nl.tudelft.trustchain.musicdao.core.repositories.model.Album
import nl.tudelft.trustchain.musicdao.ui.screens.profile.MyProfileScreenViewModel

@ExperimentalMaterialApi
@Composable
fun Drawer(
    navController: NavController,
    profileScreenViewModel: MyProfileScreenViewModel,
    database: CacheDatabase
) {
    val profile = profileScreenViewModel.profile.collectAsState()
    val albumStatsState = remember { mutableStateOf(emptyList<AlbumEntity>()) }

    LaunchedEffect(Unit) {
        albumStatsState.value = database.dao.getAll()
    }

    Column {
        Column(
            modifier =
                Modifier.padding(
                    start = 15.dp,
                    end = 15.dp,
                    top = 20.dp,
                    bottom = 20.dp
                )
        ) {
            Column(modifier = Modifier.padding(bottom = 20.dp)) {
                Box(
                    modifier =
                        Modifier
                            .size(50.dp)
                            .clip(CircleShape)
                            .background(Color.Black)
                )
            }
            Row {
                Column {
                    Text(profile.value?.name ?: "[name]", style = MaterialTheme.typography.h6)
                    Text(
                        profileScreenViewModel.publicKey(),
                        style = MaterialTheme.typography.subtitle1
                    )
                }
                Spacer(Modifier.weight(1f))
                IconButton(onClick = {}) {
                    Icon(imageVector = Icons.Default.ArrowDropDown, contentDescription = null)
                }
            }
        }
        Divider()
        Column {
            DropdownMenuItem(onClick = { navController.navigate(Screen.Debug.route) }) {
                Text("Active Torrents")
            }
            DropdownMenuItem(onClick = { navController.navigate(Screen.Settings.route) }) {
                Text("Settings")
            }
        }

        Divider()
        Column (modifier = Modifier.padding(top = 20.dp)){
            val downloadedCount = albumStatsState.value.count { it.isDownloaded }

            Text("Statistics", style = MaterialTheme.typography.h6)

            Text("Total Discovered Albums: ${albumStatsState.value.size}")
            Text("Download In Progress: ${albumStatsState.value.size - downloadedCount}")
            Text("Downloaded Albums: $downloadedCount")


        }
    }
}
