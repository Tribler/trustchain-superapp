package nl.tudelft.trustchain.musicdao.ui.screens.contribute

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Button
import androidx.compose.material.OutlinedTextField
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import kotlinx.coroutines.launch
import nl.tudelft.trustchain.musicdao.ui.SnackbarHandler
import nl.tudelft.trustchain.musicdao.ui.components.EmptyState
import nl.tudelft.trustchain.musicdao.ui.screens.profileMenu.CustomMenuItem
import nl.tudelft.trustchain.musicdao.ui.screens.wallet.BitcoinWalletViewModel

@Composable
fun ContributeCreateScreen(
    bitcoinWalletViewModel: BitcoinWalletViewModel,
    contributeViewModel: ContributeViewModel,
    navController: NavController
) {
    val amount = rememberSaveable { mutableStateOf("0.1") }
    val coroutine = rememberCoroutineScope()

    val context = LocalContext.current

    fun send() {
        val amountDouble = amount.value.toDouble()

        if (amount.value.isEmpty() || amountDouble <= 0) {
            SnackbarHandler.displaySnackbar("Please enter a valid amount")
            return
        }

        // Check if enough balance available
        val confirmedBalance = bitcoinWalletViewModel.confirmedBalance.value
        if (confirmedBalance == null || confirmedBalance.isZero || confirmedBalance.isNegative) {
            SnackbarHandler.displaySnackbar("You don't have enough funds to make a donation")
            return
        }

        coroutine.launch {
            val result = contributeViewModel.contribute(amountDouble)
//            if (result) {
            if (result) {
//                contributeViewModel.createContribution(
//                    amount.value.toLong(),
//                    context
//                )
//
//                contributeViewModel.refreshOneShot()

                SnackbarHandler.displaySnackbar("Contribution created")
                navController.popBackStack()
            } else {
                SnackbarHandler.displaySnackbar("Contribution failed")
            }
        }
    }

    Column(modifier = Modifier.padding(20.dp)) {
        Text(
            text = "Your balance is ${bitcoinWalletViewModel.confirmedBalance.value?.toFriendlyString() ?: "0.00 BTC"}",
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(bottom = 10.dp)
        )
        Text(
            text = "Amount",
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(bottom = 5.dp)
        )
        OutlinedTextField(
            value = amount.value,
            onValueChange = { amount.value = it },
            modifier = Modifier.padding(bottom = 10.dp)
        )
        Row {
            Button(
                onClick = {
                    amount.value = "0.001"
                },
                modifier = Modifier.padding(end = 10.dp)
            ) {
                Text("0.001")
            }
            Button(
                onClick = {
                    amount.value = "0.01"
                },
                modifier = Modifier.padding(end = 10.dp)
            ) {
                Text("0.01")
            }
            Button(
                onClick = {
                    amount.value = "0.1"
                },
                modifier = Modifier.padding(end = 10.dp)
            ) {
                Text("0.1")
            }
        }

        Spacer(modifier = Modifier.weight(1f))
        CustomMenuItem(text = "Confirm contribution", onClick = { send() })
    }
}
