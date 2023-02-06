package nl.tudelft.trustchain.musicdao.ui.screens.dao

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import nl.tudelft.trustchain.musicdao.core.dao.daoToColor
import nl.tudelft.trustchain.musicdao.ui.components.EmptyState
import nl.tudelft.trustchain.musicdao.ui.navigation.Screen
import nl.tudelft.trustchain.musicdao.ui.screens.profile_menu.CustomMenuItem
import com.google.accompanist.swiperefresh.SwipeRefresh
import com.google.accompanist.swiperefresh.rememberSwipeRefreshState

@ExperimentalMaterialApi
@Composable
fun DaoListScreen(navController: NavController, daoViewModel: DaoViewModel) {
    val daos by daoViewModel.daos.collectAsState()
    val isRefreshing by daoViewModel.isRefreshing.collectAsState()
    val daoPeersAmount by daoViewModel.daoPeers.collectAsState()
    val refreshState = rememberSwipeRefreshState(isRefreshing)

    val context = LocalContext.current

    SwipeRefresh(
        state = refreshState,
        onRefresh = {
            daoViewModel.refreshOneShot()
        }

    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp)
        ) {
            Column {
                CustomMenuItem(
                    text = "Create a new DAO",
                    onClick = {
                        navController.navigate(Screen.NewDaoRoute.route)
                    }
                )

                CustomMenuItem(
                    text = "Enter Bitcoin wallet",
                    onClick = {
                        navController.navigate(Screen.BitcoinWallet.route)
                    }
                )
                Spacer(modifier = Modifier.padding(2.dp))
                Column {
                    Text(text = "Discovered ${daos.size} DAOs")
                }
                Column {
                    Text(text = "Discovered $daoPeersAmount peers")
                }
                Spacer(modifier = Modifier.padding(8.dp))
                Divider()
                Spacer(modifier = Modifier.padding(8.dp))

                if (daos.isEmpty()) {
                    EmptyState(
                        firstLine = "None found.",
                        secondLine = "Please wait for DAOs to load."
                    )
                }
                LazyColumn {
                    daos.toList().sortedByDescending { it.first.timestamp }
                        .forEachIndexed { index, (_, dao) ->
                            item(index) {
                                Card(
                                    backgroundColor = MaterialTheme.colors.background,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(bottom = 20.dp),
                                    onClick = {
                                        navController.navigate(Screen.DaoDetailRoute.createRoute(dao.daoId))
                                    }
                                ) {
                                    Column(
                                        modifier = Modifier
                                            .padding(15.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        DaoIcon(dao.name, 64)
                                        Spacer(Modifier.size(20.dp))
                                        Text(dao.name, style = MaterialTheme.typography.h6)
                                        Text(
                                            "${dao.members.size} members",
                                            style = MaterialTheme.typography.body1
                                        )
                                        Text(
                                            "₿ ${dao.balance} balance",
                                            style = MaterialTheme.typography.body1
                                        )
                                        Spacer(Modifier.size(20.dp))

                                        if (!daoViewModel.userInDao(dao)) {
                                            OutlinedButton(
                                                onClick = {
                                                    daoViewModel.joinSharedWalletClicked(
                                                        dao.daoId,
                                                        context
                                                    )
                                                }
                                            ) {
                                                Text("Join (takes time)")
                                            }
                                        } else {
                                            OutlinedButton(
                                                onClick = {
                                                    daoViewModel.joinSharedWalletClicked(
                                                        dao.daoId,
                                                        context
                                                    )
                                                },
                                                enabled = false
                                            ) {
                                                Text(
                                                    "You are a member of this DAO.",
                                                    textAlign = TextAlign.Center
                                                )
                                            }
                                        }
                                        Spacer(modifier = Modifier.size(10.dp))
                                        OutlinedButton(
                                            onClick = {
                                                navController.navigate(
                                                    Screen.DaoDetailRoute.createRoute(
                                                        dao.daoId
                                                    )
                                                )
                                            }
                                        ) {
                                            Text("View DAO")
                                        }
                                    }
                                }
                            }
                        }
                }
            }
        }
    }
}

@Composable
fun DaoIcon(daoId: String, size: Int) {
    Box(
        modifier = Modifier
            .size(size.dp)
            .clip(CircleShape)
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        daoToColor(daoId).first,
                        daoToColor(daoId).second
                    )
                )

            )
    )
}
