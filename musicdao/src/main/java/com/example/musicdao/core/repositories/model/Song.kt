package com.example.musicdao.core.repositories.model

import java.io.File

data class Song(
    val title: String,
    val artist: String,
    val file: File?
)
