package nl.tudelft.trustchain.musicdao.ui.screens.profile

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
import androidx.navigation.NavController
import nl.tudelft.trustchain.musicdao.ui.components.EmptyState

@RequiresApi(Build.VERSION_CODES.O)
@ExperimentalMaterialApi
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MyProfileScreen(navController: NavController, profileScreenViewModel: MyProfileScreenViewModel) {

    val profile = profileScreenViewModel.profile.collectAsState()

    profile.value?.let {
        Profile(it, navController = navController)
    } ?: Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        EmptyState(
            firstLine = "You have not made a profile yet.",
            secondLine = "Please make one first."
        )
    }
}
