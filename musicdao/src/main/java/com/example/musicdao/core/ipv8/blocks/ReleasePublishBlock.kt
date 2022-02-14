package com.example.musicdao.core.ipv8.blocks

data class ReleasePublishBlock(
    val releaseId: String,
    val magnet: String,
    val title: String,
    val artist: String,
    val publisher: String,
    val releaseDate: String,
    val protocolVersion: String
) {
    companion object {
        val BLOCK_TYPE = "publish_release"
    }
}

