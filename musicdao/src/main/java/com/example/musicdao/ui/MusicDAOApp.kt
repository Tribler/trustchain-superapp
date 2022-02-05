package com.example.musicdao.ui

import MinimizedPlayer
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.musicdao.AppContainer
import com.example.musicdao.ui.components.player.PlayerViewModel
import com.example.musicdao.ui.release.CreateReleaseDialog
import com.google.accompanist.navigation.animation.rememberAnimatedNavController

@ExperimentalAnimationApi
@ExperimentalFoundationApi
@RequiresApi(Build.VERSION_CODES.O)
@ExperimentalMaterialApi
@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun MusicDAOApp(appContainer: AppContainer) {
    MaterialTheme(colors = MusicDAOTheme.DarkColors) {
        val navController = rememberAnimatedNavController()

        val context = LocalContext.current

        val openCreateReleaseDialog = remember { mutableStateOf(false) }
        val closeDialog = {
            openCreateReleaseDialog.value = false
        }

        val playerViewModel: PlayerViewModel =
            viewModel(factory = PlayerViewModel.provideFactory(context = context))

        Scaffold(
            scaffoldState = rememberScaffoldState(),
            floatingActionButtonPosition = FabPosition.End,
            floatingActionButton = {
                FloatingActionButton(onClick = { openCreateReleaseDialog.value = true }) {
                    Icon(
                        imageVector = Icons.Filled.AddCircle,
                        contentDescription = null,
                    )
                }
            },
            drawerContent = { Drawer() },
            content = { paddingValues ->
                Column(modifier = Modifier.padding(bottom = paddingValues.calculateBottomPadding())) {
                    Column(modifier = Modifier.weight(2f)) {
                        AppNavigation(navController, appContainer, playerViewModel)
                    }
                    MinimizedPlayer(
                        playerViewModel = playerViewModel,
                        navController = navController,
                        modifier = Modifier
                            .align(Alignment.End)
                    )
                    if (openCreateReleaseDialog.value) {
                        CreateReleaseDialog(closeDialog = closeDialog)
                    }
                }
            },
            bottomBar = { BottomNavigationBar(navController) }
        )
    }
}




