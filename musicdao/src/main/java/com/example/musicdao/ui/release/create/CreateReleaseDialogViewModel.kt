package com.example.musicdao.ui.release.create

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.musicdao.AppContainer
import com.example.musicdao.core.usecases.CreateReleaseUseCase

class CreateReleaseDialogViewModel(private val createReleaseUseCase: CreateReleaseUseCase) :
    ViewModel() {

    fun createRelease(
        artist: String,
        title: String,
        releaseDate: String,
        publisher: String,
        uris: List<Uri>,
        context: Context
    ): Boolean {
        return createReleaseUseCase.invoke(artist, title, releaseDate, publisher, uris, context)
    }

    companion object {
        fun provideFactory(
            createReleaseUseCase: CreateReleaseUseCase = AppContainer.createReleaseUseCase
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return CreateReleaseDialogViewModel(
                    createReleaseUseCase,
                ) as T
            }
        }
    }


}
