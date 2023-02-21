package nl.tudelft.trustchain.musicdao.ui.screens.settings

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import androidx.annotation.RequiresApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.ListItem
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.app.ActivityCompat.startActivityForResult
import nl.tudelft.trustchain.musicdao.AppContainer
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterialApi::class)
@Composable
@RequiresApi(Build.VERSION_CODES.O)
fun SettingsScreen(settingsScreenViewModel: SettingsScreenViewModel) {

    val coroutine = rememberCoroutineScope()
    val context = LocalContext.current

    suspend fun batchPublish(uri: Uri) {
        settingsScreenViewModel.publishBatch(uri, context)
    }

    fun openFilePickerDialog() {
        AppContainer.currentCallback = {
            coroutine.launch {
                batchPublish(it[0])
            }
        }
        val selectFilesIntent = Intent(Intent.ACTION_GET_CONTENT)
        selectFilesIntent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
        selectFilesIntent.type = "*/*"
        val chooseFileActivity = Intent.createChooser(selectFilesIntent, "Choose a file")
        startActivityForResult(AppContainer.activity, chooseFileActivity, 21, Bundle())
    }

    Column {
        ListItem(
            text = { Text(text = "Batch Publish") },
            modifier = Modifier.clickable {
                openFilePickerDialog()
            }
        )
    }
}
