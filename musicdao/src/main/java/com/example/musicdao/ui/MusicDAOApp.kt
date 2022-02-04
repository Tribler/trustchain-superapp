package com.example.musicdao.ui

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Box
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.compose.rememberNavController
import com.example.musicdao.AppContainer
import com.example.musicdao.ui.release.CreateReleaseDialog
import com.google.android.exoplayer2.ExoPlayerFactory

@ExperimentalFoundationApi
@RequiresApi(Build.VERSION_CODES.O)
@ExperimentalMaterialApi
@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun MusicDAOApp(appContainer: AppContainer) {
    MaterialTheme(colors = MusicDAOTheme.DarkColors) {
        val navController = rememberNavController()

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
                Box(modifier = Modifier.padding(bottom = paddingValues.calculateBottomPadding())) {
                    AppNavigation(navController, appContainer, exoPlayer)
                    HoveringPlayer(
                        paddingValues,
                        exoPlayer,
                        modifier = Modifier.align(Alignment.BottomCenter)
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




