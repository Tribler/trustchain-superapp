package com.example.musicdao.core.model

data class Artist(
    val publicKey: String,
    val bitcoinAddress: String,
    val name: String,
    val biography: String,
    val socials: String,
    val releaseIds: List<String>
)
