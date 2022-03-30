package com.example.musicdao.ui.screens.wallet

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.musicdao.ui.components.EmptyStateNotScrollable
import com.example.musicdao.ui.screens.profile.CustomMenuItem

@Composable
fun BitcoinWalletScreen() {

    val bitcoinWalletViewModel: BitcoinWalletViewModel = hiltViewModel()
    val confirmedBalance = bitcoinWalletViewModel.confirmedBalance.collectAsState()
    val estimatedBalance = bitcoinWalletViewModel.estimatedBalance.collectAsState()
    val syncProgress = bitcoinWalletViewModel.syncProgress.collectAsState()
    val status = bitcoinWalletViewModel.status.collectAsState()
    val faucetInProgress = bitcoinWalletViewModel.faucetInProgress.collectAsState()

    var state by remember { mutableStateOf(0) }
    val titles = listOf("ACTIONS", "TRANSACTIONS")

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
                    text = confirmedBalance.value ?: "0.00 BTC",
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
                    syncProgress.value?.let { (it / 100).toFloat() }
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

                    Column(modifier = Modifier.padding(bottom = 20.dp)) {
                        Text(text = "Syncing Progress", fontWeight = FontWeight.Bold)
                        LinearProgressIndicator(
                            bitcoinWalletViewModel.syncProgress.value?.let { (it / 100).toFloat() }
                                ?: 0f,
                            modifier = Modifier.padding(top = 10.dp)
                        )
                    }
                }
            }
            1 -> {
                Box(modifier = Modifier.fillMaxSize()) {
                    EmptyStateNotScrollable(
                        firstLine = "No Transactions",
                        secondLine = "No transactions have been made.",
                        modifier = Modifier
                            .align(Alignment.Center)
                            .padding(vertical = 50.dp)
                    )
                }
            }
        }
    }
}
