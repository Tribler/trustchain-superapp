package nl.tudelft.trustchain.musicdao.ui.screens.dao

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Card
import androidx.compose.material.MaterialTheme
import androidx.compose.material.OutlinedButton
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import nl.tudelft.trustchain.musicdao.core.dao.JoinProposal
import nl.tudelft.trustchain.musicdao.core.dao.Proposal
import nl.tudelft.trustchain.musicdao.core.dao.TransferProposal
import nl.tudelft.trustchain.musicdao.ui.SnackbarHandler
import nl.tudelft.trustchain.musicdao.ui.components.EmptyState
import com.google.accompanist.swiperefresh.SwipeRefresh
import com.google.accompanist.swiperefresh.rememberSwipeRefreshState

@Composable
fun ProposalDetailScreen(proposalId: String, daoViewModel: DaoViewModel) {
    val proposal = daoViewModel.getProposal(proposalId)

    val isRefreshing by daoViewModel.isRefreshing.collectAsState()
    val refreshState = rememberSwipeRefreshState(isRefreshing = isRefreshing)

    if (proposal != null) {
        SwipeRefresh(
            state = refreshState,
            onRefresh = {
                daoViewModel.refreshOneShot()
            }
        ) {
            ProposalDetailPure(proposal.first, daoViewModel)
        }
    } else {
        EmptyState("Not found.", proposalId)
    }
}

@Composable
fun ProposalDetailPure(proposal: Proposal, daoViewModel: DaoViewModel) {
    val context = LocalContext.current

    when (proposal) {
        is JoinProposal -> Column(
            modifier = Modifier
                .padding(20.dp)
                .verticalScroll(rememberScrollState())
        ) {
            ProposalCard(
                proposal = proposal,
                navigateToProposal = null
            )
            Spacer(modifier = Modifier.size(10.dp))

            Card {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp)
                ) {
                    val dao = daoViewModel.getDao(proposal.daoId)

                    if (proposal.isClosed()) {
                        Text(
                            "You can not sign this proposal, it has already been closed."
                        )
                    } else {
                        if (dao != null) {
                            if (daoViewModel.userInDao(dao.second)) {
                                if (!daoViewModel.hasMadeProposalVote(proposal)) {
                                    Text(
                                        "You have not signed this proposal yet, you can do so below."
                                    )
                                    Spacer(modifier = Modifier.size(10.dp))
                                    OutlinedButton(
                                        onClick = {
                                            proposal.proposalId
                                            val block =
                                                daoViewModel.getProposal(proposal.proposalId)

                                            if (block?.second != null) {
                                                daoViewModel.upvoteJoin(
                                                    context,
                                                    proposal
                                                )
                                            } else {
                                                SnackbarHandler.displaySnackbar("Could not find the proposal.")
                                            }
                                        }
                                    ) {
                                        Text("Sign this proposal")
                                    }
                                } else {
                                    Text(
                                        "You have already signed this proposal earlier, please wait."
                                    )
                                }
                            } else {
                                Text(
                                    "You can not sign this proposal since you are not a member."
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.size(10.dp))

            Card {
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
                    if (proposal.signatures.isEmpty()) {
                        Text(text = "No votes have been cast yet.")
                    }
                    proposal.signatures.map { vote ->
                        Row(
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 2.dp)
                        ) {
                            Row {
                                Box(
                                    modifier = Modifier
                                        .size(16.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colors.primary)
                                )
                                Spacer(modifier = Modifier.size(5.dp))
                                Text(
                                    vote.bitcoinPublicKey,
                                    style = MaterialTheme.typography.caption,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
            }
        }
        is TransferProposal -> Column(
            modifier = Modifier
                .padding(20.dp)
                .verticalScroll(rememberScrollState())
        ) {
            ProposalCard(
                proposal = proposal,
                navigateToProposal = null
            )
            Spacer(modifier = Modifier.size(10.dp))

            Card {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp)
                ) {
                    val dao = daoViewModel.getDao(proposal.daoId)

                    if (proposal.isClosed()) {
                        Text(
                            "You can not sign this proposal, it has already been closed."
                        )
                    } else {
                        if (dao != null) {
                            if (daoViewModel.userInDao(dao.second)) {
                                if (!daoViewModel.hasMadeProposalVote(proposal)) {
                                    Text(
                                        "You have not signed this proposal yet, you can do so below. "
                                    )
                                    Spacer(modifier = Modifier.size(10.dp))
                                    OutlinedButton(
                                        onClick = {
                                            proposal.proposalId
                                            val block =
                                                daoViewModel.getProposal(proposal.proposalId)

                                            if (block?.second != null) {
                                                daoViewModel.upvoteTransfer(
                                                    context,
                                                    proposal
                                                )
                                            } else {
                                                SnackbarHandler.displaySnackbar("Could not find the proposal.")
                                            }
                                        }
                                    ) {
                                        Text("Sign this proposal")
                                    }
                                } else {
                                    Text(
                                        "You have already signed this proposal earlier, please wait."
                                    )
                                }
                            } else {
                                Text(
                                    "You can not sign this proposal since you are not a member."
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.size(10.dp))

            Card {
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
                    if (proposal.signatures.isEmpty()) {
                        Text(text = "No votes have been cast yet.")
                    }
                    proposal.signatures.map { vote ->
                        Row(
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 2.dp)
                        ) {
                            Row {
                                Box(
                                    modifier = Modifier
                                        .size(16.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colors.primary)
                                )
                                Spacer(modifier = Modifier.size(5.dp))
                                Text(
                                    vote.bitcoinPublicKey,
                                    style = MaterialTheme.typography.caption,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
