package com.example.musicdao.ui

import VideoPlayer
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Create
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.navArgument
import androidx.navigation.compose.rememberNavController
import com.example.musicdao.AppContainer
import com.example.musicdao.ui.home.HomeScreen
import com.example.musicdao.ui.home.HomeScreenViewModel
import com.example.musicdao.ui.release.CreateReleaseDialog
import com.example.musicdao.ui.release.ReleaseScreen
import com.google.android.exoplayer2.ExoPlayerFactory
import kotlinx.coroutines.delay

@ExperimentalComposeUiApi
@ExperimentalMaterialApi
@Composable
fun MusicDAOApp() {

    val Red200 = Color(0xFF77DF7C)
    val Red300 = Color(0xFF45C761)
    val Red700 = Color(0xFF0CB829)

    val DarkColors = darkColors(
        primary = Red300,
        primaryVariant = Red700,
        onPrimary = Color.Black,
        secondary = Red300,
        onSecondary = Color.Black,
        error = Red200
    )

    MaterialTheme(colors = DarkColors) {
        val scaffoldState = rememberScaffoldState()
        var selectedBottomBarIndex by remember { mutableStateOf(0) }
        val openCreateReleaseDialog = remember { mutableStateOf(false) }

        data class BottomNavigationItem(val label: String, val route: String, val icon: ImageVector)

        val items = listOf(
            BottomNavigationItem("Home", "home", Icons.Filled.Home),
            BottomNavigationItem("Search", "search", Icons.Filled.Search),
            BottomNavigationItem("Settings", "settings", Icons.Filled.Settings)
        )

        val navController = rememberNavController()

        val context = LocalContext.current
        val exoPlayer = remember {
            ExoPlayerFactory.newSimpleInstance(context)
        }

        Scaffold(
            scaffoldState = scaffoldState,
            floatingActionButtonPosition = FabPosition.End,
            floatingActionButton = {
                FloatingActionButton(onClick = { openCreateReleaseDialog.value = true }) {
                    Icon(
                        imageVector = Icons.Filled.Create,
                        contentDescription = null,
                        tint = Color.White
                    )
                }
            },
            drawerContent = { Text(text = "drawerContent") },
            content = { paddingValues ->
                Box(modifier = Modifier.padding(bottom = paddingValues.calculateBottomPadding())) {
                    NavHost(
                        modifier = Modifier.fillMaxSize(),
                        navController = navController,
                        startDestination = items[0].route,
                        builder = {
                            composable(items[0].route) {
                                val homeScreenViewModel: HomeScreenViewModel = viewModel(
                                    factory = HomeScreenViewModel.provideFactory(
                                        AppContainer.releaseRepository,
                                        AppContainer.releaseTorrentRepository
                                    )
                                )
                                HomeScreen(
                                    navController = navController,
                                    homeScreenViewModel = homeScreenViewModel
                                )
                            }
                            composable(items[1].route) { Search() }
                            composable(items[2].route) { Debug() }
                            composable(
                                "release/{releaseId}",
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
                    Row(
                        modifier = Modifier
                            .padding(7.dp)
                            .clip(RoundedCornerShape(10))
                            .align(Alignment.BottomCenter),
                        content = {
                            VideoPlayer(exoPlayer = exoPlayer)
                        })
                }
                if (openCreateReleaseDialog.value) {
                    CreateReleaseDialog(closeDialog = {
                        openCreateReleaseDialog.value = false
                    })
                }
            },
            bottomBar = {
                BottomNavigation {
                    items.forEachIndexed { index, s ->
                        BottomNavigationItem(
                            selected = selectedBottomBarIndex == index,
                            onClick = {
                                selectedBottomBarIndex = index
                                navController.navigate(s.route)
                            },
                            icon = { Icon(s.icon, contentDescription = null) },
                            label = { Text(s.label) },
                        )
                    }
                }
            }
        )
    }

}

@Composable
fun Search() {
    Column {
        TextField(
            singleLine = true,
            value = "",
            onValueChange = {},
            placeholder = { Text("Search") },
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
fun Debug() {
    val sessionManager = AppContainer.sessionManager
    var dhtRunning by rememberSaveable { mutableStateOf(false) }
    var dhtNodes by rememberSaveable { mutableStateOf<Long>(0) }
    var uploadRate by rememberSaveable { mutableStateOf<Long>(0) }
    var downloadRate by rememberSaveable { mutableStateOf<Long>(0) }


    LaunchedEffect(Unit) {
        while (true) {
            dhtRunning = sessionManager.isDhtRunning
            dhtNodes = sessionManager.dhtNodes()
            uploadRate = sessionManager.uploadRate()
            downloadRate = sessionManager.downloadRate()
            delay(2000)
        }
    }



    Column(modifier = Modifier.padding(20.dp)) {
        Text("DHT Running: ${dhtRunning}")
        Text("DHT Peers: ${dhtNodes}")
        Text("Upload-rate: ${uploadRate}")
        Text("Download-rate: ${downloadRate}")
    }
}

