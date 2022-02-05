package com.example.musicdao.ui.home

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Divider
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.example.musicdao.ui.components.EmptyState
import com.example.musicdao.ui.components.ReleaseCover
import com.example.musicdao.ui.components.releases.ReleaseList
import com.google.accompanist.swiperefresh.SwipeRefresh
import com.google.accompanist.swiperefresh.rememberSwipeRefreshState
import java.io.File

@RequiresApi(Build.VERSION_CODES.O)
@ExperimentalMaterialApi
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun HomeScreen(navController: NavHostController, homeScreenViewModel: HomeScreenViewModel) {

    val releasesState by homeScreenViewModel.releases.observeAsState(listOf())
    val isRefreshing by homeScreenViewModel.isRefreshing.observeAsState(false)
    val refreshState = rememberSwipeRefreshState(isRefreshing)

    SwipeRefresh(state = refreshState, onRefresh = { homeScreenViewModel.refresh() }) {
        Column(modifier = Modifier.fillMaxSize()) {
            ReleaseList(releasesState = releasesState, navController = navController, header = {
                Text(
                    text = "All Releases",
                    style = MaterialTheme.typography.h6,
                    modifier = Modifier
                        .background(MaterialTheme.colors.background)
                        .fillMaxWidth()
                        .padding(20.dp)
                )
                Divider()
            }, modifier = Modifier.fillMaxSize())
            if (releasesState.isEmpty()) {
                EmptyState(
                    firstLine = "No releases found",
                    secondLine = "Make a release yourself or wait for releases to come in",
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
