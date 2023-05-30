package nl.tudelft.trustchain.musicdao.ui.screens.discover

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import nl.tudelft.trustchain.musicdao.ui.components.EmptyState
import nl.tudelft.trustchain.musicdao.ui.components.releases.ReleaseList
import com.google.accompanist.swiperefresh.SwipeRefresh
import com.google.accompanist.swiperefresh.rememberSwipeRefreshState
import nl.tudelft.trustchain.musicdao.ui.screens.search.DiscoverScreenViewModel

@RequiresApi(Build.VERSION_CODES.O)
@ExperimentalMaterialApi
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun DiscoverScreen(
    navController: NavHostController,
    discoverScreenViewModel: DiscoverScreenViewModel
) {
    val isRefreshing by discoverScreenViewModel.isRefreshing.observeAsState(false)
    val refreshState = rememberSwipeRefreshState(isRefreshing)
    val recommendations by discoverScreenViewModel.recommendations.collectAsState(listOf())
    val peerAmount by discoverScreenViewModel.peerAmount.observeAsState(0)

    SwipeRefresh(
        state = refreshState,
        onRefresh = { discoverScreenViewModel.refresh() }
    ) {
        Column {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(text = "Discovered $peerAmount peers")
            }
            ReleaseList(releasesState = recommendations, navController = navController)
            if (recommendations.isEmpty()) {
                EmptyState(
                    firstLine = "No Recommendations found",
                    secondLine = "Listen to more songs to allow us to establish your taste profile"
                )
            }
        }
    }
}
