package nl.tudelft.trustchain.musicdao.ui.screens.dao

import androidx.compose.foundation.layout.*
import androidx.compose.material.Card
import androidx.compose.material.OutlinedButton
import androidx.compose.material.OutlinedTextField
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import nl.tudelft.trustchain.musicdao.ui.SnackbarHandler

@Composable
fun DaoCreateScreen(daoViewModel: DaoViewModel, navController: NavController) {
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
                value = threshHold,
                onValueChange = { threshHold = it },
                label = { Text("Threshold (%)") },
                singleLine = false,
                placeholder = { Text("%") }
            )
            OutlinedTextField(
                value = entranceFee,
                onValueChange = { entranceFee = it },
                label = { Text("Amount (satoshi) (larger than 5000)") }
            )
            Spacer(modifier = Modifier.size(10.dp))
            OutlinedButton(
                onClick = {
                    if (entranceFee.toLong() < 5000) {
                        SnackbarHandler.displaySnackbar("Entrance fee must be larger than 5000 satoshis.")
                    } else {
                        daoViewModel.createGenesisDAO(
                            entranceFee.toLong(),
                            threshHold.toInt(),
                            context
                        )
                        daoViewModel.refreshOneShot()
                        navController.popBackStack()
                    }
                }
            ) {
                Text("Create")
            }
        }
    }
}
