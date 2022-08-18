package com.example.musicdao.ui.screens.dao

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.musicdao.ui.navigation.Screen
import com.google.accompanist.swiperefresh.SwipeRefresh
import com.google.accompanist.swiperefresh.rememberSwipeRefreshState
import java.math.BigInteger
import java.security.MessageDigest

data class ProposalSignature(
    val proposalId: String,
    val bitcoinPublicKey: String,
    val trustchainPublicKey: String
)

data class Proposal(
    val proposalCreator: String,
    val proposalId: String,
    val daoId: String,
    val proposalTime: String,
    val proposalTitle: String,
    val proposalText: String,
    val signaturesRequired: Int,
    val signatures: List<ProposalSignature>,
    val transferAmountBitcoinSatoshi: Int,
    val transferAddress: String
)

data class Member(
    val bitcoinPublicKey: String,
    val trustchainPublicKey: String
)

data class DAO(
    val daoId: String,
    val name: String,
    val about: String,
    val proposals: List<Proposal>,
    val members: List<Member>,
    val threshHold: Int,
    val entranceFee: Long,
    val previousTransaction: String,
    val balance: Long
)

fun daoToColor(daoId: String): Pair<Color, Color> {
    val sha256hash =
        MessageDigest.getInstance("SHA-256").digest(daoId.toByteArray()).takeLast(16)
            .toByteArray()
    val bits = BigInteger(sha256hash).toLong()
    return Pair(Color(bits), Color(bits + 50_000L))
}

@OptIn(ExperimentalFoundationApi::class)
@ExperimentalMaterialApi
@Composable
fun DaoListScreen(navController: NavController, daoViewModel: DaoViewModel) {
    val daos by daoViewModel.daos.collectAsState()

    val context = LocalContext.current
    val isRefreshing by rememberSaveable { mutableStateOf(false) }
    val refreshState = rememberSwipeRefreshState(isRefreshing)

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
                .verticalScroll(rememberScrollState())
        ) {
            OutlinedButton(
                onClick = { navController.navigate(Screen.NewDaoRoute.route) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 10.dp)
            ) {
                Text("Create DAO")
            }
            daos.forEach { (block, dao) ->
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
                            .padding(30.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        DaoIcon(dao.name, 64)
                        Spacer(Modifier.size(20.dp))
                        Text(dao.name, style = MaterialTheme.typography.h6)
                        Text("${dao.members.size} members", style = MaterialTheme.typography.body1)
                        Spacer(Modifier.size(20.dp))

                        if (!daoViewModel.userInDao(dao)) {
                            OutlinedButton(onClick = {
                                daoViewModel.joinSharedWalletClicked(dao.daoId, context)
                            }) {
                                Text("Join (takes time)")
                            }
                        } else {
                            Text("You are a member of this DAO.")
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
