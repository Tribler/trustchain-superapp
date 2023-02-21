package nl.tudelft.trustchain.musicdao.ui.screens.artists

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.Icon
import androidx.compose.material.ListItem
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import nl.tudelft.trustchain.musicdao.ui.components.EmptyState
import nl.tudelft.trustchain.musicdao.ui.navigation.Screen
import com.google.accompanist.swiperefresh.SwipeRefresh
import com.google.accompanist.swiperefresh.rememberSwipeRefreshState

@ExperimentalMaterialApi
@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun DiscoverArtistsScreen(navController: NavController) {
    val discoverArtistsViewModel: DiscoverArtistsViewModel = hiltViewModel()
    val artists by discoverArtistsViewModel.artists.collectAsState(listOf())
    val isRefreshing by discoverArtistsViewModel.isRefreshing.observeAsState(false)

    val refreshState = rememberSwipeRefreshState(isRefreshing)

    SwipeRefresh(state = refreshState, onRefresh = { discoverArtistsViewModel.refresh() }) {
        Column(modifier = Modifier.fillMaxSize()) {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                artists.map {
                    ListItem(
                        icon = { Icon(Icons.Default.Person, contentDescription = null) },
                        text = { Text(text = it.name) },
                        modifier = Modifier.clickable {
                            navController.navigate(Screen.Profile.createRoute(it.publicKey))
                        }
                    )
                }
            }
            if (artists.isEmpty()) {
                EmptyState(
                    firstLine = "No artists found",
                    secondLine = "Make a profile yourself or wait for profiles to come in"
                )
            }
        }
    }
}
