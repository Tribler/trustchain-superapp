package com.example.musicdao.ui

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.animation.AnimatedContentScope
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.navArgument
import androidx.navigation.compose.rememberNavController
import com.example.musicdao.ui.components.player.FullPlayerScreen
import com.example.musicdao.ui.components.player.PlayerViewModel
import com.example.musicdao.ui.debug.Debug
import com.example.musicdao.ui.home.HomeScreen
import com.example.musicdao.ui.home.HomeScreenViewModel
import com.example.musicdao.ui.ownProfile.EditProfileScreen
import com.example.musicdao.ui.ownProfile.OwnProfileScreen
import com.example.musicdao.ui.profile.ProfileMenuScreen
import com.example.musicdao.ui.profile.ProfileScreen
import com.example.musicdao.ui.release.ReleaseScreen
import com.example.musicdao.ui.search.DebugScreenViewModel
import com.example.musicdao.ui.search.SearchScreen
import com.example.musicdao.ui.search.SearchScreenViewModel
import com.google.accompanist.navigation.animation.AnimatedNavHost
import com.google.accompanist.navigation.animation.composable

sealed class Screen(val route: String) {
    object Home : Screen("home")
    object Release : Screen("release/{releaseId}") {
        fun createRoute(releaseId: String) = "release/$releaseId"
    }

    object Search : Screen("search")
    object Settings : Screen("settings")
    object Debug : Screen("debug")
    object FullPlayerScreen : Screen("fullPlayerScreen")

    object CreatorMenu : Screen("me")
    object MyProfile : Screen("me/profile")
    object EditProfile : Screen("me/edit")

    object Profile : Screen("profile/{publicKey}") {
        fun createRoute(publicKey: String) = "profile/$publicKey"
    }
}


@ExperimentalAnimationApi
@ExperimentalFoundationApi
@RequiresApi(Build.VERSION_CODES.O)
@OptIn(ExperimentalMaterialApi::class)
@Composable
fun AppNavigation(
    navController: NavHostController = rememberNavController(),
    playerViewModel: PlayerViewModel,
) {
    val homeScreenViewModel: HomeScreenViewModel = hiltViewModel()
    val searchScreenScreenViewModel: SearchScreenViewModel = hiltViewModel()
    val debugScreenViewModel: DebugScreenViewModel = hiltViewModel()


    AnimatedNavHost(
        modifier = Modifier.fillMaxSize(),
        enterTransition = { _, _ -> EnterTransition.None },
        exitTransition = { _, _ -> ExitTransition.None },
        navController = navController,
        startDestination = Screen.Home.route,
        builder = {
            composable(Screen.Home.route) {
                HomeScreen(
                    navController = navController,
                    homeScreenViewModel = homeScreenViewModel
                )
            }
            composable(Screen.Search.route) {
                SearchScreen(navController, searchScreenScreenViewModel)
            }
            composable(Screen.Debug.route) {
                Debug(debugScreenViewModel)
            }
            composable(Screen.MyProfile.route) {
                OwnProfileScreen()
            }
            composable(Screen.EditProfile.route) {
                EditProfileScreen()
            }
            composable(Screen.CreatorMenu.route) {
                ProfileMenuScreen(navController = navController)
            }
            composable(
                Screen.Profile.route,
                arguments = listOf(navArgument("publicKey") {
                    type = NavType.StringType
                })
            ) { navBackStackEntry ->
                ProfileScreen(
                    navBackStackEntry.arguments?.getString(
                        "publicKey"
                    )!!,
                )
            }
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
                    playerViewModel = playerViewModel
                )
            }
            composable(
                Screen.FullPlayerScreen.route,
                enterTransition = { _, _ ->
                    slideIntoContainer(
                        AnimatedContentScope.SlideDirection.Up,
                        animationSpec = tween(200)
                    )
                },
                exitTransition = { initial, _ ->
                    slideOutOfContainer(
                        AnimatedContentScope.SlideDirection.Down,
                        animationSpec = tween(200)
                    )
                },
            ) {
                FullPlayerScreen(playerViewModel)
            }
        })

}

