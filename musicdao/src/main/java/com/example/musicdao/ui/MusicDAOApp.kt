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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.musicdao.ui.components.player.PlayerViewModel
import com.example.musicdao.ui.navigation.AppNavigation
import com.example.musicdao.ui.screens.release.CreateReleaseDialog
import com.example.musicdao.ui.styling.MusicDAOTheme
import com.google.accompanist.navigation.animation.rememberAnimatedNavController
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

@ExperimentalAnimationApi
@ExperimentalFoundationApi
@RequiresApi(Build.VERSION_CODES.O)
@ExperimentalMaterialApi
@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun MusicDAOApp() {
    MaterialTheme(colors = MusicDAOTheme.DarkColors) {
        val navController = rememberAnimatedNavController()

        val context = LocalContext.current

        val openCreateReleaseDialog = remember { mutableStateOf(false) }
        val closeDialog = {
            openCreateReleaseDialog.value = false
        }

        val playerViewModel: PlayerViewModel =
            viewModel(factory = PlayerViewModel.provideFactory(context = context))

        val scaffoldState = rememberScaffoldState()
        SnackbarHandler.coroutineScope = rememberCoroutineScope()
        SnackbarHandler.snackbarHostState = scaffoldState.snackbarHostState

        Scaffold(
            scaffoldState = scaffoldState,
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
                        AppNavigation(navController, playerViewModel)
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
            },
        )
    }
}

object SnackbarHandler {
    var snackbarHostState: SnackbarHostState? = null
    var coroutineScope: CoroutineScope? = null

    fun displaySnackbar(text: String) {
        val snackbarHostState = snackbarHostState
        val coroutineScope = coroutineScope

        if (snackbarHostState != null && coroutineScope != null) {
            coroutineScope.launch {
                snackbarHostState.showSnackbar(message = text)
            }
        }
    }
}
