package nl.tudelft.trustchain.musicdao.ui.screens.release.create

import android.annotation.SuppressLint
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Edit
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat.startActivityForResult
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import nl.tudelft.trustchain.musicdao.AppContainer
import nl.tudelft.trustchain.musicdao.ui.SnackbarHandler
import nl.tudelft.trustchain.musicdao.ui.util.dateToLongString
import com.google.android.material.datepicker.MaterialDatePicker
import kotlinx.coroutines.launch
import java.time.Instant

@SuppressLint("UnusedMaterialScaffoldPaddingParameter")
@RequiresApi(Build.VERSION_CODES.O)
@ExperimentalComposeUiApi
@Composable
fun CreateReleaseDialog(navController: NavController) {
    val viewModel: CreateReleaseDialogViewModel = hiltViewModel()

    val fileList: MutableState<List<Uri>> = remember { mutableStateOf(listOf()) }
    val title = rememberSaveable { mutableStateOf("") }
    val artist = rememberSaveable { mutableStateOf("") }
    val date = rememberSaveable { mutableStateOf("") }

    fun openFilePickerDialog() {
        AppContainer.currentCallback = {
            fileList.value = it
        }
        val selectFilesIntent = Intent(Intent.ACTION_GET_CONTENT)
        selectFilesIntent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
        selectFilesIntent.type = "audio/*"
        selectFilesIntent.putExtra(Intent.EXTRA_MIME_TYPES, arrayOf("image/*", "audio/*"))
        val chooseFileActivity = Intent.createChooser(selectFilesIntent, "Choose a file")
        startActivityForResult(AppContainer.activity, chooseFileActivity, 21, Bundle())
    }

    val context = LocalContext.current
    fun showDatePicker() {
        val picker = MaterialDatePicker.Builder.datePicker().build()
        (context as AppCompatActivity).let {
            picker.show(it.supportFragmentManager, picker.toString())
            picker.addOnPositiveButtonClickListener {
                date.value = Instant.ofEpochMilli(it).toString()
            }
        }
    }

    val scope = rememberCoroutineScope()
    val localContext = LocalContext.current
    fun publishRelease() {
        scope.launch {
            val result = viewModel.createRelease(
                artist.value,
                title.value,
                releaseDate = Instant.now().toString(),
                uris = fileList.value,
                localContext
            )
            if (result) {
                SnackbarHandler.displaySnackbar(text = "Successfully published your release.")
                navController.popBackStack()
            } else {
                SnackbarHandler.displaySnackbar(text = "Could not publish your release.")
            }
        }
    }

    Column {
        Surface(
            modifier = Modifier
                .requiredWidth(LocalConfiguration.current.screenWidthDp.dp * 1f)
                .fillMaxSize()
                .padding(4.dp)
        ) {
            Scaffold(
                topBar = {
                    TopAppBar(
                        title = { Text("Create Release") },
                        navigationIcon = {
                            IconButton(onClick = { navController.popBackStack() }) {
                                Icon(Icons.Filled.Close, contentDescription = null)
                            }
                        }
                    )
                },
                content = {
                    Column(
                        modifier = Modifier
                            .padding(20.dp)
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = "Fill in the form below to create a new release",
                            style = MaterialTheme.typography.body1,
                            modifier = Modifier.padding(bottom = 10.dp)
                        )

                        TextField(
                            value = title.value,
                            onValueChange = { title.value = it },
                            placeholder = { Text("The title of your release") },
                            label = { Text("Title") },
                            modifier = Modifier.fillMaxWidth()
                        )

                        TextField(
                            value = artist.value,
                            onValueChange = { artist.value = it },
                            placeholder = { Text("Your artist name") },
                            label = { Text("Artist") },
                            modifier = Modifier.fillMaxWidth()
                        )

                        Row {
                            IconButton(onClick = { openFilePickerDialog() }) {
                                Icon(
                                    Icons.Default.Edit,
                                    contentDescription = null,
                                    modifier = Modifier.size(ButtonDefaults.IconSize)
                                )
                            }
                            TextField(
                                value = if (fileList.value.isEmpty()) "" else fileList.value.toString(),
                                onValueChange = {},
                                label = { Text("Files") },
                                enabled = false
                            )
                        }

                        Row {
                            IconButton(onClick = { showDatePicker() }) {
                                Icon(
                                    Icons.Default.DateRange,
                                    contentDescription = null,
                                    modifier = Modifier.size(ButtonDefaults.IconSize)
                                )
                            }
                            TextField(
                                value = dateToLongString(date.value),
                                onValueChange = {},
                                enabled = false,
                                label = { Text("Release Date") }
                            )
                        }

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(25.dp),
                            modifier = Modifier.padding(vertical = 10.dp)
                        ) {
                            Checkbox(checked = true, enabled = false, onCheckedChange = {})
                            Text("Start seeding", color = Color.Gray)
                        }

                        OutlinedButton(
                            modifier = Modifier.align(Alignment.End),
                            onClick = { publishRelease() }
                        ) {
                            Text("Create Release")
                        }
                    }
                }
            )
        }
    }
}
