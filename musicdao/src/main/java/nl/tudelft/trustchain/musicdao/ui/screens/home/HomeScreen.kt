package nl.tudelft.trustchain.musicdao.ui.screens.home

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import nl.tudelft.trustchain.musicdao.ui.components.EmptyState
import nl.tudelft.trustchain.musicdao.ui.components.releases.ReleaseList
import nl.tudelft.trustchain.musicdao.ui.screens.search.SearchScreenViewModel
import com.google.accompanist.swiperefresh.SwipeRefresh
import com.google.accompanist.swiperefresh.rememberSwipeRefreshState

@RequiresApi(Build.VERSION_CODES.O)
@ExperimentalMaterialApi
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun HomeScreen(
    navController: NavHostController,
    screenViewModel: SearchScreenViewModel
) {
    val isRefreshing by screenViewModel.isRefreshing.observeAsState(false)
    val releases by screenViewModel.searchResult.collectAsState(listOf())
    val searchQuery by screenViewModel.searchQuery.collectAsState()
    val refreshState = rememberSwipeRefreshState(isRefreshing)
    val peerAmount by screenViewModel.peerAmount.observeAsState(0)
    val totalReleaseAmount by screenViewModel.totalReleaseAmount.observeAsState(0)

    SwipeRefresh(
        state = refreshState,
        onRefresh = { screenViewModel.refresh() }
    ) {
        Column {
            TextField(
                value = searchQuery,
                onValueChange = {
                    screenViewModel.searchDebounced(it)
                },
                placeholder = { Text("Search") },
                trailingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = null
                    )
                },
                maxLines = 1,
                modifier = Modifier.fillMaxWidth()
            )
            Divider()
            Column(modifier = Modifier.padding(16.dp)) {
                Text(text = "Discovered $totalReleaseAmount releases")
                Text(text = "Discovered $peerAmount peers")
            }
            ReleaseList(releasesState = releases, navController = navController)
            if (releases.isEmpty()) {
                EmptyState(
                    firstLine = "No releases found",
                    secondLine = "Make a release yourself or wait for releases to come in"
                )
            }
        }
    }
}
