package nl.tudelft.trustchain.musicdao.ui

import MinimizedPlayer
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import nl.tudelft.trustchain.musicdao.ui.components.player.PlayerViewModel
import nl.tudelft.trustchain.musicdao.ui.navigation.AppNavigation
import nl.tudelft.trustchain.musicdao.ui.navigation.Drawer
import nl.tudelft.trustchain.musicdao.ui.navigation.Screen
import nl.tudelft.trustchain.musicdao.ui.screens.profile.MyProfileScreenViewModel
import nl.tudelft.trustchain.musicdao.ui.styling.MusicDAOTheme
import com.google.accompanist.navigation.animation.rememberAnimatedNavController
import nl.tudelft.trustchain.musicdao.ui.navigation.BottomNavigationBar

@ExperimentalAnimationApi
@ExperimentalFoundationApi
@RequiresApi(Build.VERSION_CODES.O)
@ExperimentalMaterialApi
@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun MusicDAOApp() {
    MaterialTheme(colors = MusicDAOTheme.DarkColors, shapes = MusicDAOTheme.Shapes) {
        val navController = rememberAnimatedNavController()

        val context = LocalContext.current

        val playerViewModel: PlayerViewModel =
            viewModel(factory = PlayerViewModel.provideFactory(context = context))
        val ownProfileViewScreenModel: MyProfileScreenViewModel = hiltViewModel()

        val scaffoldState = rememberScaffoldState()
        SnackbarHandler.coroutineScope = rememberCoroutineScope()
        SnackbarHandler.snackbarHostState = scaffoldState.snackbarHostState

        Scaffold(
            scaffoldState = scaffoldState,
            floatingActionButtonPosition = FabPosition.End,
            floatingActionButton = {
                FloatingActionButton(
                    onClick = {
                        val x = navController as NavController
                        x.navigate(Screen.FullPlayerScreen.route)
                    }
                ) {
                    Icon(
                        imageVector = Icons.Filled.PlayArrow,
                        contentDescription = null
                    )
                }
            },
            drawerContent = { Drawer(navController, ownProfileViewScreenModel) },
            content = { paddingValues ->
                Column(modifier = Modifier.padding(bottom = paddingValues.calculateBottomPadding())) {
                    Column(modifier = Modifier.weight(2f)) {
                        AppNavigation(navController, playerViewModel, ownProfileViewScreenModel)
                    }
                    MinimizedPlayer(
                        playerViewModel = playerViewModel,
                        navController = navController,
                        modifier = Modifier
                            .align(Alignment.End)
                    )
                }
            },
            bottomBar = { BottomNavigationBar(navController) },
            snackbarHost = {
                SnackbarHost(it) { data ->
                    Snackbar(
                        snackbarData = data,
                        backgroundColor = MaterialTheme.colors.secondary,
                        contentColor = MaterialTheme.colors.onSecondary,
                        actionColor = MaterialTheme.colors.onSecondary
                    )
                }
            }
        )
    }
}
