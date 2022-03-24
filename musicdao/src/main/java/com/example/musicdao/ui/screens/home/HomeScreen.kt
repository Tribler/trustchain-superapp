package com.example.musicdao.ui.screens.home

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import com.example.musicdao.ui.components.EmptyState
import com.example.musicdao.ui.components.releases.ReleaseList
import com.google.accompanist.swiperefresh.SwipeRefresh
import com.google.accompanist.swiperefresh.rememberSwipeRefreshState

@RequiresApi(Build.VERSION_CODES.O)
@ExperimentalMaterialApi
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun HomeScreen(navController: NavHostController, homeScreenViewModel: HomeScreenViewModel) {

    val releasesState by homeScreenViewModel.releases.observeAsState(listOf())
    val isRefreshing by homeScreenViewModel.isRefreshing.observeAsState(false)
    val refreshState = rememberSwipeRefreshState(isRefreshing)

    SwipeRefresh(
        state = refreshState,
        onRefresh = { homeScreenViewModel.refresh() }
    ) {
        val modifier = if (releasesState.isEmpty()) Modifier else Modifier.fillMaxSize()

        Column(modifier = Modifier.fillMaxSize()) {
            ReleaseList(
                releasesState = releasesState,
                navController = navController,
                modifier = modifier
            )
            if (releasesState.isEmpty()) {
                EmptyState(
                    firstLine = "No releases found",
                    secondLine = "Make a release yourself or wait for releases to come in",
                )
            }
        }
    }
}
