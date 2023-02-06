package nl.tudelft.trustchain.musicdao.ui.screens.search

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.material.TextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavController
import nl.tudelft.trustchain.musicdao.ui.components.releases.ReleaseList

@ExperimentalFoundationApi
@OptIn(ExperimentalMaterialApi::class)
@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun SearchScreen(navController: NavController, screenViewModel: SearchScreenViewModel) {
    val releases by screenViewModel.searchResult.collectAsState(listOf())
    val searchQuery by screenViewModel.searchQuery.collectAsState()

    Column {
        TextField(
            value = searchQuery,
            onValueChange = {
                screenViewModel.searchDebounced(it)
            },
            placeholder = { Text("Search") },
            trailingIcon = { Icon(imageVector = Icons.Default.Search, contentDescription = null) },
            maxLines = 1,
            modifier = Modifier.fillMaxWidth()
        )
        ReleaseList(releasesState = releases, navController = navController)
    }
}
