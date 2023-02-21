package nl.tudelft.trustchain.musicdao.ui.screens.dao

import android.app.Activity
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import nl.tudelft.trustchain.musicdao.ui.SnackbarHandler

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun ProposalCreateScreen(daoId: String, daoViewModel: DaoViewModel, navController: NavController) {
    var satoshi by rememberSaveable { mutableStateOf("6000") }
    var address by rememberSaveable { mutableStateOf("mkKcu9VCNTAerxbZLXvLSGBTBiLwqGqcDL") }
    var chosenArtist by rememberSaveable { mutableStateOf("") }

    val local = LocalContext.current

    val dao = daoViewModel.getDao(daoId)

    var expanded by remember { mutableStateOf(false) }

    fun newProposal() {
        if (dao != null) {
            // validation
            if (satoshi.toLong() <= 5000) {
                SnackbarHandler.displaySnackbar("Amount should be larger than 5000.")
                return
            }

            if ((daoViewModel.getDao(daoId)?.second?.balance ?: 0) < satoshi.toLong()) {
                SnackbarHandler.displaySnackbar("Not enough balance.")
                return
            }

            daoViewModel.transferFundsClickedByMe(
                address,
                satoshi.toLong(),
                dao.first.calculateHash(),
                local,
                local as Activity
            )

            daoViewModel.refreshOneShot()
            navController.popBackStack()
        }
    }

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
                value = satoshi,
                onValueChange = { satoshi = it },
                label = { Text("Amount (satoshi) (larger than 5000)") }
            )
            OutlinedTextField(
                value = address,
                onValueChange = { address = it },
                label = { Text("Bitcoin Address") }
            )
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = { expanded = true }) {
                        Icon(
                            Icons.Default.Person,
                            contentDescription = "Localized description",
                            modifier = Modifier.size(ButtonDefaults.IconSize)
                        )
                    }
                    OutlinedTextField(
                        value = chosenArtist,
                        onValueChange = { chosenArtist = it },
                        label = { Text("Artist") },
                        enabled = false
                    )
                }
                DropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false }
                ) {
                    daoViewModel.getListsOfArtists().map { artist ->
                        DropdownMenuItem(
                            onClick = {
                                address = artist.bitcoinAddress; expanded = false; chosenArtist =
                                    artist.name
                            }
                        ) {
                            Text(artist.name)
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.size(10.dp))
            OutlinedButton(
                onClick = {
                    newProposal()
                }
            ) {
                Text("Create")
            }
        }
    }
}
