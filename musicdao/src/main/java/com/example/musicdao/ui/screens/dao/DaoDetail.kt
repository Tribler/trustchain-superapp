package com.example.musicdao.ui.screens.dao

import android.app.Activity
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import com.example.musicdao.ui.components.EmptyState
import com.example.musicdao.ui.navigation.Screen

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

@RequiresApi(Build.VERSION_CODES.N)
@OptIn(ExperimentalAnimationApi::class)
@Composable
fun DaoDetailPure(
    dao: DAO,
    navigateToProposal: (proposalId: String) -> Unit = {},
    navigateToNewProposal: (daoId: String) -> Unit = {},
    daoViewModel: DaoViewModel
) {
    var state by remember { mutableStateOf(0) }

    val titles = mapOf<String, String>(
        "list" to "Proposals",
        "new" to "New Proposal",
        "about" to "About"
    )

    val navhostDao: NavHostController = rememberNavController()
    val context = LocalContext.current
    val coroutine = rememberCoroutineScope()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Card() {
            Column() {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(20.dp)
                ) {
                    DaoIcon(daoId = dao.name, size = 64)
                    Spacer(modifier = Modifier.size(20.dp))
                    Column() {
                        Text(dao.name, style = MaterialTheme.typography.h6)
                        Text("${dao.members.size} members", style = MaterialTheme.typography.body1)
                    }
                    Spacer(Modifier.weight(1f))
                    OutlinedButton(onClick = {}) {
                        Text("Join")
                    }
                }

                TabRow(selectedTabIndex = state) {
                    titles.onEachIndexed() { index, (key, title) ->
                        Tab(
                            text = { Text(title) },
                            selected = state == index,
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

//        OutlinedButton(
//            onClick = { navigateToNewProposal(dao.daoId) },
//            modifier = Modifier
//                .fillMaxWidth()
//                .padding(top = 10.dp)
//        ) {
//            Text("Create Proposal")
//        }

        Column() {
            when (state) {
                0 -> {
                    Proposals(dao, navigateToProposal)
                }
                1 -> {
                    NewProposal(dao)
                }
                2 -> {
                    About(dao)
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
                Column() {
                    dao.members.map { member ->
                        Text(text = member.trustchainPublicKey.toString())
                    }
                }
            }
        }
    }
}

@Composable
fun NewProposal(dao: DAO) {
    Column() {
    }
}

@Composable
fun Proposals(dao: DAO, navigateToProposal: (proposalId: String) -> Unit) {
    Column() {
        dao.proposals.map { proposal ->
            ProposalCard(proposal, navigateToProposal)
        }
    }
}

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun ProposalCard(proposal: Proposal, navigateToProposal: (proposalId: String) -> Unit) {
    Card(
        modifier = Modifier.padding(top = 10.dp),
        onClick = {
            navigateToProposal(proposal.proposalId)
        }
    ) {
        Column(
            modifier = Modifier
                .padding(20.dp)
                .fillMaxWidth()
        ) {
            Text(
                "by ${proposal.proposalCreator}",
                style = MaterialTheme.typography.caption,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                proposal.proposalTitle,
                style = MaterialTheme.typography.h6,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                proposal.proposalText,
                style = MaterialTheme.typography.body2,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.size(10.dp))
            Text(
                "For ${proposal.transferAmountBitcoinSatoshi} satoshi to ${proposal.transferAddress} ${proposal.transferAmountBitcoinSatoshi}",
                style = MaterialTheme.typography.caption,
                softWrap = true
            )
            Spacer(modifier = Modifier.size(10.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Info,
                    tint = MaterialTheme.colors.primary,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.size(5.dp))
                Text(
                    "Closed, ${proposal.signatures.size} of ${proposal.signaturesRequired} signatures",
                    style = MaterialTheme.typography.caption
                )
            }
        }
    }
}

@Composable
fun ProposalDetailScreen(proposalId: String, daoViewModel: DaoViewModel) {
    val proposal = daoViewModel.getProposal(proposalId)

    if (proposal != null) {
        ProposalDetailPure(proposal)
    } else {
        EmptyState("Not found.", proposalId)
    }
}

@Composable
fun ProposalDetailPure(proposal: Proposal) {
    Column(
        modifier = Modifier
            .padding(20.dp)
    ) {
        Card() {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                Text(
                    "by ${proposal.proposalCreator}",
                    style = MaterialTheme.typography.caption,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    proposal.proposalTitle,
                    style = MaterialTheme.typography.h6,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (proposal.proposalText != "") {
                    Text(
                        proposal.proposalText,
                        style = MaterialTheme.typography.body2,
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Spacer(modifier = Modifier.size(10.dp))
                Text(
                    "For ${proposal.transferAmountBitcoinSatoshi} satoshi to ${proposal.transferAddress} ${proposal.transferAmountBitcoinSatoshi}",
                    style = MaterialTheme.typography.caption,
                    softWrap = true
                )
                Spacer(modifier = Modifier.size(10.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        tint = MaterialTheme.colors.primary,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.size(5.dp))
                    Text(
                        "Closed, ${proposal.signatures.size} of ${proposal.signaturesRequired} signatures",
                        style = MaterialTheme.typography.caption
                    )
                }
            }
        }

        Spacer(modifier = Modifier.size(10.dp))

        Card() {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                Text(
                    "You have not signed this proposal yet, you can do so below.",
                    style = MaterialTheme.typography.body2
                )
                Spacer(modifier = Modifier.size(10.dp))
                OutlinedButton(onClick = { /*TODO*/ }) {
                    Text("Sign this proposal")
                }
            }
        }

        Spacer(modifier = Modifier.size(10.dp))

        Card() {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                Text(
                    "Votes",
                    style = MaterialTheme.typography.subtitle2,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.size(10.dp))
                proposal.signatures.map { vote ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 10.dp)
                    ) {
                        Row() {
                            Box(
                                modifier = Modifier
                                    .size(16.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colors.primary)
                            )
                            Spacer(modifier = Modifier.size(5.dp))
                            Text(
                                vote.trustchainPublicKey,
                                style = MaterialTheme.typography.caption,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        Text(
                            "Upvote",
                            style = MaterialTheme.typography.caption
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun NewDaoScreen(daoViewModel: DaoViewModel, navController: NavController) {
    var name by rememberSaveable { mutableStateOf("") }
    var threshHold by rememberSaveable { mutableStateOf("") }
    var entranceFee by rememberSaveable { mutableStateOf("") }

    val context = LocalContext.current

    Card(
        modifier = Modifier
            .padding(20.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Title") }
            )
            OutlinedTextField(
                value = threshHold,
                onValueChange = { threshHold = it },
                label = { Text("Threshold") },
                singleLine = false,
                maxLines = 5
            )
            OutlinedTextField(
                value = entranceFee,
                onValueChange = { entranceFee = it },
                label = { Text("Amount (satoshi)") }
            )
            Spacer(modifier = Modifier.size(10.dp))
            OutlinedButton(onClick = {
                daoViewModel.createGenesisDAO(entranceFee.toLong(), threshHold.toInt(), context)
                daoViewModel.refreshOneShot()
                navController.popBackStack()
            }) {
                Text("Create")
            }
        }
    }
}

@Composable
fun NewProposalScreen(daoId: String, daoViewModel: DaoViewModel, navController: NavController) {
    var title by rememberSaveable { mutableStateOf("") }
    var about by rememberSaveable { mutableStateOf("Lorem ipsum") }
    var satoshi by rememberSaveable { mutableStateOf("6000") }
    var address by rememberSaveable { mutableStateOf("my4VbT52jXKdpbjZz9sSvxMbmbFC86mMDb") }

    val local = LocalContext.current

    Card(
        modifier = Modifier
            .padding(20.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("Title") }
            )
            OutlinedTextField(
                value = about,
                onValueChange = { about = it },
                label = { Text("About\n\n\n\n\n\n") },
                singleLine = false,
                maxLines = 5
            )
            OutlinedTextField(
                value = satoshi,
                onValueChange = { satoshi = it },
                label = { Text("Amount (satoshi)") }
            )
            OutlinedTextField(
                value = address,
                onValueChange = { address = it },
                label = { Text("Address") }
            )
            Spacer(modifier = Modifier.size(10.dp))
            OutlinedButton(onClick = {
                daoViewModel.transferFundsClicked(
                    bitcoinPublicKey = address,
                    satoshiTransferAmount = satoshi.toLong(),
                    blockHash = daoViewModel.getDao(daoId)!!.first.calculateHash(),
                    context = local,
                    activityRequired = local as Activity
                )
                daoViewModel.refreshOneShot()
                navController.popBackStack()
            }) {
                Text("Create")
            }
        }
    }
}
