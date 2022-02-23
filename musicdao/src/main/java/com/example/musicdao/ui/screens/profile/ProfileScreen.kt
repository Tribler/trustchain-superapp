package com.example.musicdao.ui.screens.profile

import android.app.Activity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.MaterialTheme
import androidx.compose.material.OutlinedButton
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.musicdao.MusicActivity
import com.example.musicdao.core.model.Artist
import dagger.hilt.android.EntryPointAccessors

@Composable
fun ProfileScreen(publicKey: String) {

    val viewModelFactory = EntryPointAccessors.fromActivity(
        LocalContext.current as Activity,
        MusicActivity.ViewModelFactoryProvider::class.java
    ).profileScreenViewModelFactory()

    val viewModel: ProfileScreenViewModel = viewModel(
        factory = ProfileScreenViewModel.provideFactory(viewModelFactory, publicKey = publicKey)
    )

    val profile = viewModel.profile.collectAsState()

    profile.value?.let {
        Profile(artist = it)
    } ?: Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator()
    }

}

