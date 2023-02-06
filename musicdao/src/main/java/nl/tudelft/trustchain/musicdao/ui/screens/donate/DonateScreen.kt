package nl.tudelft.trustchain.musicdao.ui.screens.donate

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Button
import androidx.compose.material.OutlinedTextField
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import nl.tudelft.trustchain.musicdao.ui.SnackbarHandler
import nl.tudelft.trustchain.musicdao.ui.components.EmptyState
import nl.tudelft.trustchain.musicdao.ui.screens.profile_menu.CustomMenuItem
import nl.tudelft.trustchain.musicdao.ui.screens.wallet.BitcoinWalletViewModel
import kotlinx.coroutines.launch

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun DonateScreen(bitcoinWalletViewModel: BitcoinWalletViewModel, publicKey: String, navController: NavController) {
    val donateScreenViewModel: DonateScreenViewModel = hiltViewModel()
    val artist = donateScreenViewModel.artist.collectAsState()
    val amount = rememberSaveable { mutableStateOf("0.1") }
    val coroutine = rememberCoroutineScope()

    LaunchedEffect(publicKey) {
        coroutine.launch {
            donateScreenViewModel.setArtist(publicKey)
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    fun send() {
        // Check if enough balance available
        val confirmedBalance = bitcoinWalletViewModel.confirmedBalance.value
        if (confirmedBalance == null || confirmedBalance.isZero || confirmedBalance.isNegative) {
            SnackbarHandler.displaySnackbar("You don't have enough funds to make a donation")
            return
        }

        coroutine.launch {
            val result = bitcoinWalletViewModel.donate(publicKey, amount.value)
            if (result) {
                SnackbarHandler.displaySnackbar("Donation sent")
                navController.popBackStack()
            } else {
                SnackbarHandler.displaySnackbar("Donation failed")
            }
        }
    }

    if (artist.value == null) {
        EmptyState(firstLine = "404", secondLine = "This artist has not published a key you can donate to.")
        return
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
        OutlinedTextField(value = amount.value, onValueChange = { amount.value = it }, modifier = Modifier.padding(bottom = 10.dp))
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
        CustomMenuItem(text = "Confirm Send", onClick = { send() })
    }
}
