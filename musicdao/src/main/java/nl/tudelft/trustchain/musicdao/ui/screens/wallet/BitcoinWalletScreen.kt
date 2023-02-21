package nl.tudelft.trustchain.musicdao.ui.screens.wallet

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.ArrowForward
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import nl.tudelft.trustchain.musicdao.core.wallet.UserWalletTransaction
import nl.tudelft.trustchain.musicdao.ui.components.EmptyState
import nl.tudelft.trustchain.musicdao.ui.components.EmptyStateNotScrollable
import nl.tudelft.trustchain.musicdao.ui.screens.profile_menu.CustomMenuItem
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun BitcoinWalletScreen(bitcoinWalletViewModel: BitcoinWalletViewModel) {
    val confirmedBalance = bitcoinWalletViewModel.confirmedBalance.collectAsState()
    val estimatedBalance = bitcoinWalletViewModel.estimatedBalance.collectAsState()
    val syncProgress = bitcoinWalletViewModel.syncProgress.collectAsState()
    val status = bitcoinWalletViewModel.status.collectAsState()
    val faucetInProgress = bitcoinWalletViewModel.faucetInProgress.collectAsState()
    val walletTransactions = bitcoinWalletViewModel.walletTransactions.collectAsState()
    val isStarted = bitcoinWalletViewModel.isStarted.collectAsState()

    var state by remember { mutableStateOf(0) }
    val titles = listOf("ACTIONS", "TRANSACTIONS")

    if (!isStarted.value) {
        EmptyState(
            firstLine = "Your wallet is not started yet.",
            secondLine = "Please, wait for the wallet to be started.",
            loadingIcon = true
        )
        return
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp)
                .background(MaterialTheme.colors.primary)
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .align(
                        Alignment.BottomStart
                    )
            ) {
                Text(
                    text = confirmedBalance.value?.toFriendlyString() ?: "0.00 BTC",
                    style = MaterialTheme.typography.h6
                )
                Text(
                    text = "${estimatedBalance.value ?: "0.00 BTC"} (Estimated)",
                    style = MaterialTheme.typography.subtitle1
                )
            }
            Row(
                modifier = Modifier
                    .padding(20.dp)
                    .align(
                        Alignment.TopEnd
                    ),
                horizontalArrangement = Arrangement.Center
            ) {
                Text(
                    "Sync Progress",
                    modifier = Modifier
                        .padding(end = 15.dp)
                        .align(Alignment.CenterVertically)
                )
                LinearProgressIndicator(
                    syncProgress.value?.let { (it.toFloat() / 100) }
                        ?: 0f,
                    color = MaterialTheme.colors.onPrimary,
                    modifier = Modifier
                        .align(Alignment.CenterVertically)
                        .fillMaxWidth()
                )
            }
        }

        TabRow(selectedTabIndex = state) {
            titles.forEachIndexed { index, title ->
                Tab(
                    onClick = { state = index },
                    selected = (index == state),
                    text = { Text(title) }
                )
            }
        }

        when (state) {
            0 -> {
                Column(modifier = Modifier.padding(horizontal = 20.dp)) {
                    CustomMenuItem(
                        text = "Request from faucet",
                        onClick = {
                            bitcoinWalletViewModel.requestFaucet()
                        },
                        disabled = faucetInProgress.value
                    )
                    CustomMenuItem(
                        text = "Send",
                        onClick = { },
                        enabled = false
                    )
                    CustomMenuItem(
                        text = "Receive",
                        onClick = { },
                        enabled = false
                    )

                    Column(modifier = Modifier.padding(bottom = 20.dp)) {
                        Text(text = "Public Key", fontWeight = FontWeight.Bold)
                        Text(text = bitcoinWalletViewModel.publicKey.value ?: "No Public Key")
                    }

                    Column(modifier = Modifier.padding(bottom = 20.dp)) {
                        Text(text = "Wallet Status", fontWeight = FontWeight.Bold)
                        Text(text = status.value ?: "No Status")
                    }
                }
            }
            1 -> {
                Box(modifier = Modifier.fillMaxSize()) {
                    if (walletTransactions.value.isEmpty()) {
                        EmptyStateNotScrollable(
                            firstLine = "No Transactions",
                            secondLine = "No transactions have been made.",
                            modifier = Modifier
                                .align(Alignment.Center)
                                .padding(vertical = 50.dp)
                        )
                    } else {
                        Column(modifier = Modifier.fillMaxSize()) {
                            walletTransactions.value.map {
                                TransactionItem(
                                    userWalletTransaction = it
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun TransactionItem(userWalletTransaction: UserWalletTransaction) {
    ListItem(
        icon = {
            Icon(
                imageVector = if (userWalletTransaction.value.isPositive) {
                    Icons.Outlined.ArrowForward
                } else {
                    Icons.Outlined.ArrowBack
                },
                contentDescription = null
            )
        },
        overlineText = {
            Text(
                text = dateToString(userWalletTransaction.date),
                style = MaterialTheme.typography.caption
            )
        },
        text = {
            val text = if (userWalletTransaction.value.isPositive) {
                "Received"
            } else {
                "Sent"
            }
            Text(text = text)
        },
        secondaryText = {
            Text(text = userWalletTransaction.transaction.txId.toString())
        },
        trailing = {
            Text(
                text = userWalletTransaction.value.toFriendlyString(),
                style = TextStyle(
                    color = if (userWalletTransaction.value.isPositive) {
                        Color.Green
                    } else {
                        Color.Red
                    }
                )
            )
        }
    )
}

fun dateToString(date: Date): String {
    val formatter = SimpleDateFormat("dd MMMM, yyyy, HH:mm", Locale.US)
    return formatter.format(date)
}
