package com.example.musicdao.ui.components.releases

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
import androidx.compose.material.icons.filled.Menu
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.musicdao.core.model.Album
import com.example.musicdao.ui.Screen
import com.example.musicdao.ui.components.ReleaseCover
import com.example.musicdao.ui.dateToShortString

@RequiresApi(Build.VERSION_CODES.O)
@ExperimentalMaterialApi
@ExperimentalFoundationApi
@Composable
fun ReleaseList(
    releasesState: List<Album>,
    navController: NavController,
    header: @Composable (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {

    LazyColumn(modifier = modifier) {
        if (header != null) {
            stickyHeader {
                header()
            }
        }
        items(releasesState) {
            ListItem(
                text = { Text(it.title) },
                secondaryText = {
                    Row {
                        Text("Album - ${it.artist}")
                        if (it.songs != null && it.songs.isNotEmpty()) {
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
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            dateToShortString(it.releaseDate.toString()),
                            modifier = Modifier.padding(end = 10.dp)
                        )
                        Icon(
                            imageVector = Icons.Default.Menu,
                            contentDescription = null
                        )
                    }
                },
                modifier = Modifier.clickable {
                    navController.navigate(
                        Screen.Release.createRoute(
                            it.id
                        )
                    )
                },
                icon = { ReleaseCover(it.cover, modifier = Modifier.size(40.dp)) }
            )
            Divider()
        }
        if (releasesState.isNotEmpty()) {
            item("end") {
                Column(modifier = Modifier.height(100.dp)) {}
            }
        }
    }
}
