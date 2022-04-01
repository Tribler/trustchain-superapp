package com.example.musicdao.ui.screens.donate

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.material.OutlinedTextField
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.musicdao.ui.SnackbarHandler
import com.example.musicdao.ui.screens.profile.CustomMenuItem
import com.example.musicdao.ui.screens.wallet.BitcoinWalletViewModel

@Composable
fun DonateScreen(bitcoinWalletViewModel: BitcoinWalletViewModel, publicKey: String) {

    val amount = rememberSaveable { mutableStateOf("1") }

    fun send() {
        SnackbarHandler.displaySnackbar("Did not send a donation.")
    }

    Column(modifier = Modifier.padding(20.dp)) {

        Text(
            text = "Current Balance is ${bitcoinWalletViewModel.confirmedBalance.value ?: "0.00 BTC"}",
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(bottom = 5.dp)
        )
        Text(
            text = "Amount",
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(bottom = 5.dp)
        )
        OutlinedTextField(value = amount.value, onValueChange = { amount.value = it })

        Spacer(modifier = Modifier.weight(1f))
        CustomMenuItem(text = "Confirm Send", onClick = { send() })
    }
}
