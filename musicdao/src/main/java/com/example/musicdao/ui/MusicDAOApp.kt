package com.example.musicdao.ui

import VideoPlayer
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Create
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.example.musicdao.AppContainer
import com.example.musicdao.ui.release.CreateReleaseDialog
import com.google.accompanist.navigation.animation.rememberAnimatedNavController
import com.google.android.exoplayer2.ExoPlayerFactory

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
        val exoPlayer = remember {
            ExoPlayerFactory.newSimpleInstance(context)
        }

        val openCreateReleaseDialog = remember { mutableStateOf(false) }
        val closeDialog = {
            openCreateReleaseDialog.value = false
        }

        Scaffold(
            scaffoldState = rememberScaffoldState(),
            floatingActionButtonPosition = FabPosition.End,
            floatingActionButton = {
                FloatingActionButton(onClick = { openCreateReleaseDialog.value = true }) {
                    Icon(
                        imageVector = Icons.Filled.Create,
                        contentDescription = null,
                    )
                }
            },
            drawerContent = { Drawer() },
            content = { paddingValues ->
                Column(modifier = Modifier.padding(bottom = paddingValues.calculateBottomPadding())) {
                    Column(modifier = Modifier.weight(2f)) {
                        AppNavigation(navController, appContainer, exoPlayer)
                    }
                    VideoPlayer(
                        exoPlayer = exoPlayer,
                        navController = navController,
                        modifier = Modifier
                            .align(Alignment.End)
                            .weight(1f)
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




