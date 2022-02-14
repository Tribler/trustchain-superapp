package com.example.musicdao.core.model

import java.io.File
import java.time.Instant

data class Album(
    val id: String,
    val magnet: String,
    val title: String,
    val artist: String,
    val publisher: String,
    val releaseDate: Instant,
    val songs: List<Song>?,
    val cover: File?,
    val root: File?,
)
