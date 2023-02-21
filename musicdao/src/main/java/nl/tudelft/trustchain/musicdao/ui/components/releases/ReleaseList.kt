package nl.tudelft.trustchain.musicdao.ui.components.releases

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import nl.tudelft.trustchain.musicdao.core.repositories.model.Album
import nl.tudelft.trustchain.musicdao.ui.components.ReleaseCover
import nl.tudelft.trustchain.musicdao.ui.navigation.Screen

@RequiresApi(Build.VERSION_CODES.O)
@ExperimentalMaterialApi
@ExperimentalFoundationApi
@Composable
fun ReleaseList(
    releasesState: List<Album>,
    navController: NavController,
    modifier: Modifier = Modifier
) {
    LazyColumn(modifier = modifier) {
        item(0) {
            Divider()
        }
        items(releasesState) {
            ReleaseListItem(album = it, navController = navController)
            Divider()
        }
        if (releasesState.isNotEmpty()) {
            item("end") {
                Column(modifier = Modifier.height(100.dp)) {}
            }
        }
    }
}

@RequiresApi(Build.VERSION_CODES.O)
@ExperimentalMaterialApi
@ExperimentalFoundationApi
@Composable
fun NonLazyReleaseList(
    releasesState: List<Album>,
    navController: NavController,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        releasesState.map {
            ReleaseListItem(album = it, navController = navController)
        }
    }
}

@RequiresApi(Build.VERSION_CODES.O)
@ExperimentalMaterialApi
@ExperimentalFoundationApi
@Composable
fun ReleaseListItem(album: Album, navController: NavController) {
    ListItem(
        text = { Text(text = album.title, maxLines = 1, overflow = TextOverflow.Ellipsis) },
        secondaryText = {
            Row {
                Text(
                    text = "Album - ${album.artist}",
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (album.songs != null && album.songs.isNotEmpty()) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        tint = MaterialTheme.colors.primary,
                        contentDescription = null,
                        modifier = Modifier
                            .align(Alignment.CenterVertically)
                            .padding(start = 5.dp)
                            .size(15.dp)
                    )
                }
            }
        },
        trailing = {
            IconButton(onClick = { }) {
                Icon(imageVector = Icons.Default.MoreVert, contentDescription = null)
            }
        },
        modifier = Modifier.clickable {
            navController.navigate(
                Screen.Release.createRoute(
                    album.id
                )
            )
        },
        icon = { ReleaseCover(album.cover, modifier = Modifier.size(40.dp)) }
    )
}
