package com.example.musicdao.ui.home

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.example.musicdao.ui.Screen
import com.example.musicdao.ui.components.ReleaseCover
import java.io.File

@ExperimentalMaterialApi
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun HomeScreen(navController: NavHostController, homeScreenViewModel: HomeScreenViewModel) {

    val releasesState by homeScreenViewModel.releases.observeAsState(listOf())

    Column(modifier = Modifier.fillMaxSize()) {
        LazyColumn {
            stickyHeader {
                Text(
                    text = "All Releases",
                    style = MaterialTheme.typography.h6,
                    modifier = Modifier
                        .background(MaterialTheme.colors.background)
                        .fillMaxWidth(  )
                        .padding(20.dp)
                )
                Divider()
            }
            items(releasesState) {
                ListItem(
                    text = { Text(it.releaseBlock.title) },
                    secondaryText = { Text("Album - ${it.releaseBlock.artist}") },
                    trailing = {
                        Icon(
                            imageVector = Icons.Default.Menu,
                            contentDescription = null
                        )
                    },
                    modifier = Modifier.clickable {
                        navController.navigate(
                            Screen.Release.createRoute(
                                it.releaseBlock.releaseId
                            )
                        )
                    },
                    icon = { ReleaseCover(it.cover, modifier = Modifier.size(40.dp)) }
                )
                Divider()
            }
            item("end") {
                Column(modifier = Modifier.height(100.dp)) {}
            }
        }
    }
}

@Composable
fun ReleaseCoverButton(title: String, artist: String, file: File?, modifier: Modifier) {
    Column(modifier) {
        ReleaseCover(
            file, modifier = Modifier
                .height(115.dp)
                .aspectRatio(1f)
                .clip(RoundedCornerShape(5))
        )
        Text(
            text = title,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(top = 5.dp)
        )
        Text(
            text = "Album - $artist",
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.width(115.dp)
        )
    }
}
