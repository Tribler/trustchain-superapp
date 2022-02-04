package com.example.musicdao.ui

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.navArgument
import androidx.navigation.compose.rememberNavController
import com.example.musicdao.AppContainer
import com.example.musicdao.ui.debug.Debug
import com.example.musicdao.ui.home.HomeScreen
import com.example.musicdao.ui.home.HomeScreenViewModel
import com.example.musicdao.ui.release.ReleaseScreen
import com.google.android.exoplayer2.SimpleExoPlayer

sealed class Screen(val route: String) {
    object Home : Screen("home")
    object Release : Screen("release/{releaseId}") {
        fun createRoute(releaseId: String) = "release/$releaseId"
    }

    object Search : Screen("search")
    object Settings : Screen("settings")
    object Debug : Screen("debug")
}


@OptIn(ExperimentalMaterialApi::class)
@Composable
fun AppNavigation(
    navController: NavHostController = rememberNavController(),
    appContainer: AppContainer,
    exoPlayer: SimpleExoPlayer,
) {
    NavHost(
        modifier = Modifier.fillMaxSize(),
        navController = navController,
        startDestination = Screen.Home.route,
        builder = {
            composable(Screen.Home.route) {
                val homeScreenViewModel: HomeScreenViewModel = viewModel(
                    factory = HomeScreenViewModel.provideFactory(
                        appContainer.releaseRepository,
                    )
                )
                HomeScreen(
                    navController = navController,
                    homeScreenViewModel = homeScreenViewModel
                )
            }
            composable(Screen.Search.route) { Search() }
            composable(Screen.Debug.route) { Debug() }
            composable(
                Screen.Release.route,
                arguments = listOf(navArgument("releaseId") {
                    type = NavType.StringType
                })
            ) { navBackStackEntry ->
                ReleaseScreen(
                    navBackStackEntry.arguments?.getString(
                        "releaseId"
                    )!!,
                    exoPlayer = exoPlayer
                )
            }
        })

}

@Composable
fun Search() {
}
