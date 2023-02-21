package nl.tudelft.trustchain.musicdao.ui.screens.profile

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.OutlinedButton
import androidx.compose.material.Text
import androidx.compose.material.TextField
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import nl.tudelft.trustchain.musicdao.ui.SnackbarHandler
import kotlinx.coroutines.launch

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun EditProfileScreen(navController: NavController) {
    val ownProfileViewScreenModel: MyProfileScreenViewModel = hiltViewModel()
    val profile = ownProfileViewScreenModel.profile.collectAsState()

    val name = remember { mutableStateOf(profile.value?.name) }
    val bitcoinPublicKey = remember { mutableStateOf(profile.value?.bitcoinAddress) }
    val biography = remember { mutableStateOf(profile.value?.biography) }
    val socials = remember { mutableStateOf(profile.value?.socials) }
    val coroutine = rememberCoroutineScope()

    fun save() {
        coroutine.launch {
            val result = ownProfileViewScreenModel.publishEdit(
                name = name.value ?: "",
                bitcoinAddress = bitcoinPublicKey.value ?: "",
                socials = socials.value ?: "",
                biography = biography.value ?: ""
            )
            if (result) {
                navController.popBackStack()
                SnackbarHandler.displaySnackbar(text = "Successfully published your profile")
            } else {
                SnackbarHandler.displaySnackbar(text = "Could not publish, please fill in all fields")
            }
        }
    }

    Column(
        modifier = Modifier
            .padding(20.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Column(modifier = Modifier.padding(bottom = 20.dp)) {
            Text(text = "Name", fontWeight = FontWeight.Bold)
            TextField(
                value = name.value ?: "",
                onValueChange = { name.value = it }
            )
        }

        Column(modifier = Modifier.padding(bottom = 20.dp)) {
            Text(text = "Public Key", fontWeight = FontWeight.Bold)
            TextField(
                value = ownProfileViewScreenModel.publicKey(),
                enabled = false,
                onValueChange = {}
            )
        }

        Column(modifier = Modifier.padding(bottom = 20.dp)) {
            Text(text = "Bitcoin Public Key", fontWeight = FontWeight.Bold)
            TextField(
                value = bitcoinPublicKey.value ?: "",
                onValueChange = { bitcoinPublicKey.value = it },
                enabled = false
            )
        }

        Column(modifier = Modifier.padding(bottom = 20.dp)) {
            Text(text = "Socials", fontWeight = FontWeight.Bold)
            TextField(
                value = socials.value ?: "",
                onValueChange = { socials.value = it }
            )
        }

        Column(modifier = Modifier.padding(bottom = 20.dp)) {
            Text(text = "Biography", fontWeight = FontWeight.Bold)
            TextField(
                value = biography.value ?: "",
                onValueChange = { biography.value = it }
            )
        }

        OutlinedButton(
            onClick = {
                save()
            }
        ) {
            Text("Save")
        }
    }
}
