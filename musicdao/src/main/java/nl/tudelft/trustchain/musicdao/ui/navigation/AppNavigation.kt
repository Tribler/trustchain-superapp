package nl.tudelft.trustchain.musicdao.ui.navigation

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
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import nl.tudelft.trustchain.musicdao.ui.components.player.FullPlayerScreen
import nl.tudelft.trustchain.musicdao.ui.components.player.PlayerViewModel
import nl.tudelft.trustchain.musicdao.ui.screens.artists.DiscoverArtistsScreen
import nl.tudelft.trustchain.musicdao.ui.screens.debug.Debug
import nl.tudelft.trustchain.musicdao.ui.screens.donate.DonateScreen
import nl.tudelft.trustchain.musicdao.ui.screens.home.HomeScreen
import nl.tudelft.trustchain.musicdao.ui.screens.release.create.CreateReleaseDialog
import nl.tudelft.trustchain.musicdao.ui.screens.release.ReleaseScreen
import nl.tudelft.trustchain.musicdao.ui.screens.search.SearchScreen
import nl.tudelft.trustchain.musicdao.ui.screens.search.SearchScreenViewModel
import nl.tudelft.trustchain.musicdao.ui.screens.settings.SettingsScreen
import nl.tudelft.trustchain.musicdao.ui.screens.settings.SettingsScreenViewModel
import nl.tudelft.trustchain.musicdao.ui.screens.wallet.BitcoinWalletScreen
import nl.tudelft.trustchain.musicdao.ui.screens.wallet.BitcoinWalletViewModel
import com.google.accompanist.navigation.animation.AnimatedNavHost
import com.google.accompanist.navigation.animation.composable
import nl.tudelft.trustchain.musicdao.ui.screens.dao.*
import nl.tudelft.trustchain.musicdao.ui.screens.debug.DebugScreenViewModel
import nl.tudelft.trustchain.musicdao.ui.screens.profile.EditProfileScreen
import nl.tudelft.trustchain.musicdao.ui.screens.profile.MyProfileScreen
import nl.tudelft.trustchain.musicdao.ui.screens.profile.MyProfileScreenViewModel
import nl.tudelft.trustchain.musicdao.ui.screens.profile.ProfileScreen
import nl.tudelft.trustchain.musicdao.ui.screens.profile_menu.ProfileMenuScreen

@ExperimentalAnimationApi
@ExperimentalFoundationApi
@RequiresApi(Build.VERSION_CODES.O)
@OptIn(ExperimentalMaterialApi::class, ExperimentalComposeUiApi::class)
@Composable
fun AppNavigation(
    navController: NavHostController = rememberNavController(),
    playerViewModel: PlayerViewModel,
    ownProfileViewScreenModel: MyProfileScreenViewModel
) {
    val bitcoinWalletViewModel: BitcoinWalletViewModel = hiltViewModel()
    val daoViewModel: DaoViewModel = hiltViewModel()

    daoViewModel.initManager()

    AnimatedNavHost(
        modifier = Modifier.fillMaxSize(),
        enterTransition = { EnterTransition.None },
        exitTransition = { ExitTransition.None },
        navController = navController,
        startDestination = Screen.Home.route,
        builder = {
            composable(Screen.Home.route) {
                val searchScreenViewModel: SearchScreenViewModel = hiltViewModel()
                HomeScreen(
                    navController = navController,
                    screenViewModel = searchScreenViewModel
                )
            }
            composable(Screen.Search.route) {
                val searchScreenScreenViewModel: SearchScreenViewModel = hiltViewModel()
                SearchScreen(navController, searchScreenScreenViewModel)
            }
            composable(Screen.Debug.route) {
                val debugScreenViewModel: DebugScreenViewModel = hiltViewModel()

                Debug(debugScreenViewModel)
            }
            composable(Screen.MyProfile.route) {
                MyProfileScreen(navController = navController, ownProfileViewScreenModel)
            }
            composable(Screen.EditProfile.route) {
                EditProfileScreen(navController = navController)
            }
            composable(Screen.CreatorMenu.route) {
                ProfileMenuScreen(navController = navController)
            }
            composable(Screen.BitcoinWallet.route) {
                BitcoinWalletScreen(bitcoinWalletViewModel = bitcoinWalletViewModel)
            }
            composable(
                Screen.Donate.route,
                arguments = listOf(
                    navArgument("publicKey") {
                        type = NavType.StringType
                    }
                )
            ) { navBackStackEntry ->
                DonateScreen(
                    bitcoinWalletViewModel = bitcoinWalletViewModel,
                    navBackStackEntry.arguments?.getString(
                        "publicKey"
                    )!!,
                    navController = navController
                )
            }
            composable(Screen.DiscoverArtists.route) {
                DiscoverArtistsScreen(navController = navController)
            }
            composable(Screen.Settings.route) {
                val settingsScreenViewModel: SettingsScreenViewModel = hiltViewModel()
                SettingsScreen(settingsScreenViewModel)
            }
            composable(Screen.DaoRoute.route) {
                DaoListScreen(navController = navController, daoViewModel = daoViewModel)
            }
            composable(Screen.NewDaoRoute.route) {
                DaoCreateScreen(daoViewModel = daoViewModel, navController = navController)
            }
            composable(Screen.CreateRelease.route) {
                CreateReleaseDialog(navController = navController)
            }

            composable(
                Screen.DaoDetailRoute.route,
                arguments = listOf(
                    navArgument("daoId") {
                        type = NavType.StringType
                    }
                )
            ) { navBackStackEntry ->
                DaoDetailScreen(
                    navController = navController,
                    daoId = navBackStackEntry.arguments?.getString(
                        "daoId"
                    )!!,
                    daoViewModel = daoViewModel
                )
            }
            composable(
                Screen.ProposalDetailRoute.route,
                arguments = listOf(
                    navArgument("proposalId") {
                        type = NavType.StringType
                    }
                )
            ) { navBackStackEntry ->
                ProposalDetailScreen(
                    navBackStackEntry.arguments?.getString(
                        "proposalId"
                    )!!,
                    daoViewModel = daoViewModel
                )
            }
            composable(
                Screen.NewProposalRoute.route,
                arguments = listOf(
                    navArgument("daoId") {
                        type = NavType.StringType
                    }
                )
            ) { navBackStackEntry ->
                ProposalCreateScreen(
                    navBackStackEntry.arguments?.getString(
                        "daoId"
                    )!!,
                    daoViewModel = daoViewModel,
                    navController = navController
                )
            }

            composable(
                Screen.Profile.route,
                arguments = listOf(
                    navArgument("publicKey") {
                        type = NavType.StringType
                    }
                )
            ) { navBackStackEntry ->
                ProfileScreen(
                    navBackStackEntry.arguments?.getString(
                        "publicKey"
                    )!!,
                    navController = navController
                )
            }
            composable(
                Screen.Release.route,
                arguments = listOf(
                    navArgument("releaseId") {
                        type = NavType.StringType
                    }
                )
            ) { navBackStackEntry ->
                ReleaseScreen(
                    navBackStackEntry.arguments?.getString(
                        "releaseId"
                    )!!,
                    playerViewModel = playerViewModel,
                    navController = navController
                )
            }
            composable(
                Screen.FullPlayerScreen.route,
                enterTransition = {
                    slideIntoContainer(
                        AnimatedContentScope.SlideDirection.Up,
                        animationSpec = tween(200)
                    )
                },
                exitTransition = {
                    slideOutOfContainer(
                        AnimatedContentScope.SlideDirection.Down,
                        animationSpec = tween(200)
                    )
                }
            ) {
                FullPlayerScreen(playerViewModel)
            }
        }
    )
}
