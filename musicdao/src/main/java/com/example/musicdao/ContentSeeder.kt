package com.example.musicdao

import android.util.Log
import com.frostwire.jlibtorrent.SessionManager
import com.frostwire.jlibtorrent.TorrentInfo
import org.apache.commons.io.FileUtils
import java.io.File
import java.util.*

class ContentSeeder(private val sessionManager: SessionManager, private val saveDir: File) {
    private val maxTorrentThreads = 10

    fun start() {
        if (!saveDir.isDirectory) throw Error("Content seeder active in non-directory")
        saveDir.listFiles()?.forEachIndexed { index, file ->
            if (index >= maxTorrentThreads) return
            if (file.name.endsWith(".torrent")) {
                val torrentInfo = TorrentInfo(file)
                if (torrentInfo.isValid) {
                    sessionManager.download(torrentInfo, saveDir)
                }
            }
        }

        val timer = Timer()
        val monitor = object : TimerTask() {
            override fun run() {
                Log.d("TorrentSessionManager", "UP: ${sessionManager.uploadRate()}, DOWN: ${sessionManager.downloadRate()}, DHT: ${sessionManager.dhtNodes()}")
            }
        }
        timer.schedule(monitor, 1000, 5000)
    }
    fun add(torrentInfo: TorrentInfo) {
        val torrentFile = File("$saveDir/${torrentInfo.name()}.torrent")
        if (torrentInfo.isValid) {
            if (!torrentFile.isFile) {
                FileUtils.copyInputStreamToFile(torrentInfo.bencode().inputStream(), torrentFile)
            }
            sessionManager.download(torrentInfo, saveDir)
        }
    }

}
