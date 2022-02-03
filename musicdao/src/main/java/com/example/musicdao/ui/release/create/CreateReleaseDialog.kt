package com.example.musicdao.ui.release

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.app.ActivityCompat.startActivityForResult
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.musicdao.AppContainer
import com.example.musicdao.ui.release.create.CreateReleaseDialogViewModel


@ExperimentalComposeUiApi
@Composable
fun CreateReleaseDialog(closeDialog: () -> Unit) {

    var fileList: MutableState<List<Uri>> = remember { mutableStateOf(listOf<Uri>()) }
    var title = rememberSaveable { mutableStateOf("") }
    var artist = rememberSaveable { mutableStateOf("") }

    fun start() {
        AppContainer.currentCallback = {
            fileList.value = it
        }
        val selectFilesIntent = Intent(Intent.ACTION_GET_CONTENT)
        selectFilesIntent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
        selectFilesIntent.type = "audio/*"
        val chooseFileActivity = Intent.createChooser(selectFilesIntent, "Choose a file")
        startActivityForResult(AppContainer.activity, chooseFileActivity, 21, Bundle())
    }

    val viewModel: CreateReleaseDialogViewModel =
        viewModel(factory = CreateReleaseDialogViewModel.provideFactory())


    val localContext = LocalContext.current
    fun publishRelease() {
        val res = viewModel.createRelease(
            artist.value,
            title.value,
            releaseDate = "DEFAULT",
            publisher = "DEFAULT",
            uris = fileList.value,
            localContext
        )
        if (res) {
            closeDialog()
        }
    }

    Dialog(
        onDismissRequest = {
            closeDialog()
        },
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Surface(
            modifier = Modifier
                .requiredWidth(LocalConfiguration.current.screenWidthDp.dp * 1f)
                .fillMaxSize()
                .padding(4.dp)
        ) {
            Scaffold(
                topBar = {
                    TopAppBar(
                        title = { Text("New Release") },
                        navigationIcon = {
                            IconButton(onClick = { closeDialog() }) {
                                Icon(Icons.Filled.Close, contentDescription = null)
                            }
                        },
                        actions = {
                            IconButton(onClick = {
                                publishRelease()
                            }) {
                                Text(
                                    "Save",
                                    style = MaterialTheme.typography.button
                                )
                            }
                        }
                    )
                },
                content = {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        TextField(
                            singleLine = true,
                            value = title.value,
                            onValueChange = {
                                title.value = it
                            },
                            label = { Text("Title") },
                            modifier = Modifier.fillMaxWidth()
                        )
                        TextField(
                            singleLine = true,
                            value = artist.value,
                            onValueChange = {
                                artist.value = it
                            },
                            label = { Text("Artist") },
                            modifier = Modifier.fillMaxWidth()
                        )
                        fileList.value.forEach {
                            Text(it.toString())
                        }
                        Button(onClick = { start() }) {
                            Text("File Picker")
                        }
                    }

                })
        }
    }
}


