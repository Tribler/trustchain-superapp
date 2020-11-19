package com.example.musicdao.net

import com.example.musicdao.util.Util
import com.frostwire.jlibtorrent.SessionManager
import com.frostwire.jlibtorrent.TorrentInfo
import org.apache.commons.io.FileUtils
import java.io.File
import java.util.*

lateinit var contentSeederInstance: ContentSeeder

/**
 * This maintains an implementation of a strategy of libtorrent seeding.
 * Currently, all files in the cache directory are being seeded.
 */
class ContentSeeder(private val sessionManager: SessionManager, private val saveDir: File) {
    private val maxTorrentThreads = 10
    private var started = false

    fun start(): Int {
        var count = 0
        if (started) return count
        started = true
        if (!saveDir.isDirectory) throw Error("Content seeder active in non-directory")
        val fileList = saveDir.listFiles()
        if (fileList !is Array<File>) return count
        Arrays.sort(fileList) { a, b -> a.lastModified().compareTo(b.lastModified()) }
        saveDir.listFiles()?.forEachIndexed { index, file ->
            if (index >= maxTorrentThreads) return count
            if (file.name.endsWith(".torrent")) {
                val torrentInfo = TorrentInfo(file)
                if (torrentInfo.isValid) {
                    count += 1
                    // 'Downloading' the torrent file also starts seeding it after download has
                    // already been completed
                    // We only seed torrents that have previously already been fully downloaded,
                    // so that this does not clash with the TorrentStream library
                    if (Util.isTorrentCompleted(torrentInfo, saveDir)) {
                        sessionManager.download(torrentInfo, saveDir)
                    }
                }
            }
        }
        return count
    }

    /**
     * Create, save and seed a torrent file, based on a TorrentInfo object
     */
    fun add(torrentInfo: TorrentInfo, torrentInfoName: String): Boolean {
        val torrentFile = File("$saveDir/$torrentInfoName.torrent")
        if (torrentInfo.isValid) {
            if (!torrentFile.isFile) {
                FileUtils.copyInputStreamToFile(torrentInfo.bencode().inputStream(), torrentFile)
            }
            // TODO enable seeding of all files that you have locally. Currently doing this
            //  clashes with the TorrentStreaming library somehow
            sessionManager.download(torrentInfo, saveDir)
            return true
        }
        return false
    }

    companion object {
        fun getInstance(sessionManager: SessionManager, cacheDir: File): ContentSeeder {
            if (!::contentSeederInstance.isInitialized) {
                contentSeederInstance = ContentSeeder(sessionManager, cacheDir)
            }
            return contentSeederInstance
        }
    }
}
