package nl.tudelft.trustchain.musicdao.ui.screens.profile

import android.app.Activity
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import nl.tudelft.trustchain.musicdao.MusicActivity
import nl.tudelft.trustchain.musicdao.ui.components.EmptyState
import dagger.hilt.android.EntryPointAccessors

@ExperimentalMaterialApi
@ExperimentalFoundationApi
@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun ProfileScreen(publicKey: String, navController: NavController) {

    val viewModelFactory = EntryPointAccessors.fromActivity(
        LocalContext.current as Activity,
        MusicActivity.ViewModelFactoryProvider::class.java
    ).profileScreenViewModelFactory()

    val viewModel: ProfileScreenViewModel = viewModel(
        factory = ProfileScreenViewModel.provideFactory(viewModelFactory, publicKey = publicKey)
    )

    val profile = viewModel.profile.collectAsState()
    val releases = viewModel.releases.collectAsState()

    profile.value?.let {
        Profile(artist = it, releases = releases.value, navController = navController)
    } ?: Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        EmptyState(firstLine = "404", secondLine = "This artist has not published  any information yet.")
        return
    }
}
