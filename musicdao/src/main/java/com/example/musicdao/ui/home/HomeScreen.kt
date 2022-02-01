package com.example.musicdao.ui.home

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
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
    val scrollState = rememberScrollState()

    Column(modifier = Modifier.fillMaxSize()) {
        LazyColumn(modifier = Modifier.padding(20.dp)) {
            item(1) {
                Text(
                    text = "Recommended",
                    style = MaterialTheme.typography.h6,
                    modifier = Modifier
                        .background(MaterialTheme.colors.background)
                        .padding(bottom = 10.dp)
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(15.dp),
                    modifier = Modifier
                        .horizontalScroll(scrollState)
                        .padding(bottom = 20.dp)
                ) {
                    releasesState.forEach {
                        val cover = homeScreenViewModel.getCover(it)
                        ReleaseCoverButton(
                            it.title,
                            it.artist,
                            cover,
                            modifier = Modifier.clickable { navController.navigate("release/${it.torrentInfoName}") })
                    }
                }

            }
            stickyHeader {
                Text(
                    text = "All Releases",
                    style = MaterialTheme.typography.h6,
                    modifier = Modifier.background(MaterialTheme.colors.background)
                )
            }
            items(releasesState) {
                val cover = homeScreenViewModel.getCover(it)
                ListItem(
                    text = { Text(it.title) },
                    secondaryText = { Text(it.artist) },
                    modifier = Modifier.clickable {
                        navController.navigate(
                            Screen.Release.createRoute(
                                it.torrentInfoName
                            )
                        )
                    },
                    icon = {
                        IconButton(onClick = {}) {
                            ReleaseCover(
                                cover, modifier = Modifier
                                    .clip(RoundedCornerShape(5))
                            )

                        }
                    }
                )
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
