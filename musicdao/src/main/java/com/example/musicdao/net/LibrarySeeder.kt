package com.example.musicdao.net

import com.frostwire.jlibtorrent.TorrentInfo
import java.io.File

class LibrarySeeder {
    public fun seedTrack(file: File) {
        val info = TorrentInfo(file)
    }
}
