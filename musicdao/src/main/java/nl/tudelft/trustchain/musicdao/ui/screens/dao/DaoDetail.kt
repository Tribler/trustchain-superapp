package nl.tudelft.trustchain.musicdao.ui.screens.dao

import android.annotation.SuppressLint
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import nl.tudelft.trustchain.musicdao.core.dao.DAO
import nl.tudelft.trustchain.musicdao.core.dao.JoinProposal
import nl.tudelft.trustchain.musicdao.core.dao.Proposal
import nl.tudelft.trustchain.musicdao.core.dao.TransferProposal
import nl.tudelft.trustchain.musicdao.ui.components.EmptyState
import nl.tudelft.trustchain.musicdao.ui.navigation.Screen
import nl.tudelft.trustchain.musicdao.ui.util.Chip
import com.google.accompanist.swiperefresh.SwipeRefresh
import com.google.accompanist.swiperefresh.rememberSwipeRefreshState

@RequiresApi(Build.VERSION_CODES.N)
@Composable
fun DaoDetailScreen(navController: NavController, daoId: String, daoViewModel: DaoViewModel) {
    val navigateToProposal = fun(proposalId: String) {
        navController.navigate(Screen.ProposalDetailRoute.createRoute(proposalId))
    }

    val navigateToNewProposal = fun(daoId: String) {
        navController.navigate(Screen.NewProposalRoute.createRoute(daoId))
    }

    val dao = daoViewModel.getDao(daoId)

    if (dao != null) {
        DaoDetailPure(dao.second, navigateToProposal, navigateToNewProposal, daoViewModel)
    } else {
        EmptyState("Not found.", daoId)
    }
}

@SuppressLint("UnrememberedMutableState")
@RequiresApi(Build.VERSION_CODES.N)
@Composable
fun DaoDetailPure(
    daoInitial: DAO,
    navigateToProposal: (proposalId: String) -> Unit = {},
    navigateToNewProposal: (daoId: String) -> Unit = {},
    daoViewModel: DaoViewModel
) {
    var state by remember { mutableStateOf(0) }

    val titles = mapOf(
        "list" to "Proposals",
        "new" to "New Proposal",
        "about" to "About"
    )

    val isRefreshing by daoViewModel.isRefreshing.collectAsState()
    val refreshState = rememberSwipeRefreshState(isRefreshing)

    val daos by daoViewModel.daos.collectAsState()
    val daoDerivedState = derivedStateOf {
        daos.toList().find { daoInitial.daoId == it.second.daoId }?.second
    }
    val dao = daoDerivedState.value

    if (dao == null) {
        EmptyState("Not found.", daoInitial.daoId)
        return
    }

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
            Card {
                Column {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(20.dp)
                    ) {
                        DaoIcon(daoId = dao.name, size = 64)
                        Spacer(modifier = Modifier.size(20.dp))
                        Column {
                            Text(
                                dao.name,
                                style = MaterialTheme.typography.h6,
                                overflow = TextOverflow.Ellipsis,
                                maxLines = 1
                            )
                            Text(
                                "${dao.members.size} member(s)",
                                style = MaterialTheme.typography.body1
                            )
                            Text("₿ ${dao.balance}", style = MaterialTheme.typography.body1)
                        }
                        Spacer(Modifier.weight(1f))
                        OutlinedButton(onClick = {}) {
                            Text("Join")
                        }
                    }

                    TabRow(selectedTabIndex = state) {
                        titles.onEachIndexed { index, (key, title) ->
                            Tab(
                                text = { Text(title) },
                                selected = state == index,
                                enabled = !(!daoViewModel.userInDao(dao) && key == "new"),
                                onClick = {
                                    when (key) {
                                        "list" -> state = 0
                                        "new" -> {
                                            state = 1
                                            navigateToNewProposal(dao.daoId)
                                        }
                                        "about" -> state = 2
                                    }
                                }
                            )
                        }
                    }
                }
            }

            Column {
                when (state) {
                    0 -> {
                        ProposalList(dao, navigateToProposal, daoViewModel)
                    }
                    2 -> {
                        About(dao)
                    }
                }
            }
        }
    }
}

@Composable
fun ProposalList(
    dao: DAO,
    navigateToProposal: (proposalId: String) -> Unit,
    daoViewModel: DaoViewModel
) {
    if (!daoViewModel.userInDao(dao)) {
        Card(
            modifier = Modifier
                .padding(top = 10.dp)
                .fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .fillMaxWidth()
            ) {
                Text("You are not a member of this DAO, you will not see any proposals.")
            }
        }

        return
    }

    Column {
        dao.proposals.map { proposal ->
            ProposalCard(proposal.key, navigateToProposal)
        }
    }
}

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun ProposalCard(proposal: Proposal, navigateToProposal: ((proposalId: String) -> Unit)?) {
    when (proposal) {
        is JoinProposal -> {
            Card(
                modifier = Modifier.padding(top = 10.dp),
                onClick = {
                    if (navigateToProposal != null) {
                        navigateToProposal(proposal.proposalId)
                    }
                }
            ) {
                Column(
                    modifier = Modifier
                        .padding(20.dp)
                        .fillMaxWidth()
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Chip(contentDescription = "", label = "Join Proposal", color = Color.Black)
                        if (proposal.isClosed()) {
                            Chip(contentDescription = "", label = "Closed", color = Color.Red)
                        } else {
                            Chip(contentDescription = "", label = "Open")
                        }
                    }
                    Spacer(modifier = Modifier.size(10.dp))
                    Text(
                        proposal.proposalId,
                        style = MaterialTheme.typography.h6,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.size(10.dp))
                    Column(modifier = Modifier.padding(bottom = 20.dp)) {
                        Text(text = "Creator", fontWeight = FontWeight.Bold)
                        Text(
                            text = proposal.proposalCreator,
                            overflow = TextOverflow.Ellipsis,
                            maxLines = 1
                        )
                    }
                    Column(modifier = Modifier.padding(bottom = 20.dp)) {
                        Text(text = "Creation Time", fontWeight = FontWeight.Bold)
                        Text(text = proposal.proposalTime)
                    }
                    Column(modifier = Modifier.padding(bottom = 20.dp)) {
                        Text(text = "Amount", fontWeight = FontWeight.Bold)
                        Text(text = proposal.transferAmountBitcoinSatoshi.toString())
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            tint = MaterialTheme.colors.primary,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.size(5.dp))
                        Text(
                            "${proposal.signatures.size} of ${proposal.signaturesRequired} signatures",
                            style = MaterialTheme.typography.caption
                        )
                    }
                }
            }
        }
        is TransferProposal -> Card(
            modifier = Modifier.padding(top = 10.dp),
            onClick = {
                if (navigateToProposal != null) {
                    navigateToProposal(proposal.proposalId)
                }
            }
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .fillMaxWidth()
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Chip(
                        contentDescription = "",
                        label = "Transfer Funds Proposal",
                        color = Color.Black
                    )
                    if (proposal.isClosed()) {
                        Chip(contentDescription = "", label = "Closed", color = Color.Red)
                    } else {
                        Chip(contentDescription = "", label = "Open")
                    }
                }
                Spacer(modifier = Modifier.size(10.dp))
                Text(
                    proposal.proposalId,
                    style = MaterialTheme.typography.h6,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.size(10.dp))
                Column(modifier = Modifier.padding(bottom = 20.dp)) {
                    Text(text = "Creator", fontWeight = FontWeight.Bold)
                    Text(
                        text = proposal.proposalCreator,
                        overflow = TextOverflow.Ellipsis,
                        maxLines = 1
                    )
                }
                Column(modifier = Modifier.padding(bottom = 20.dp)) {
                    Text(text = "Creation Time", fontWeight = FontWeight.Bold)
                    Text(text = proposal.proposalTime)
                }
                Column(modifier = Modifier.padding(bottom = 20.dp)) {
                    Text(text = "Amount", fontWeight = FontWeight.Bold)
                    Text(text = proposal.transferAmountBitcoinSatoshi.toString())
                }
                Column(modifier = Modifier.padding(bottom = 20.dp)) {
                    Text(text = "To", fontWeight = FontWeight.Bold)
                    Text(
                        text = proposal.transferAddress,
                        overflow = TextOverflow.Ellipsis,
                        maxLines = 1
                    )
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        tint = MaterialTheme.colors.primary,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.size(5.dp))
                    Text(
                        "${proposal.signatures.size} of ${proposal.signaturesRequired} signatures",
                        style = MaterialTheme.typography.caption
                    )
                }
            }
        }
    }
}

@Composable
fun About(dao: DAO) {
    Card(
        modifier = Modifier.padding(top = 10.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Column(modifier = Modifier.padding(bottom = 20.dp)) {
                Text(text = "Name", fontWeight = FontWeight.Bold)
                Text(text = dao.name)
            }
            Column(modifier = Modifier.padding(bottom = 20.dp)) {
                Text(text = "ID", fontWeight = FontWeight.Bold)
                Text(text = dao.daoId)
            }
            Column(modifier = Modifier.padding(bottom = 20.dp)) {
                Text(text = "Balance", fontWeight = FontWeight.Bold)
                Text(text = dao.balance.toString())
            }
            Column(modifier = Modifier.padding(bottom = 20.dp)) {
                Text(text = "Members", fontWeight = FontWeight.Bold)
                Column {
                    dao.members.map { member ->
                        Text(
                            text = member.trustchainPublicKey,
                            modifier = Modifier.padding(bottom = 5.dp)
                        )
                    }
                }
            }
        }
    }
}
