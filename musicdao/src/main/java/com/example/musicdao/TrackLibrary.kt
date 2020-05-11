package com.example.musicdao

import android.content.Context
import android.content.res.Resources
import android.net.Uri
import com.frostwire.jlibtorrent.SessionManager
import com.frostwire.jlibtorrent.SessionParams
import com.frostwire.jlibtorrent.SettingsPack
import com.frostwire.jlibtorrent.TorrentInfo
import com.frostwire.jlibtorrent.swig.settings_pack
import com.turn.ttorrent.client.SharedTorrent
import java.io.File
import java.io.FileOutputStream
import java.nio.file.Files
import java.nio.file.Paths
import java.util.*


class TrackLibrary {
    private val torrentSessionManager = SessionManager()
    private val torrentSettingsPack = SettingsPack()

    fun getUploadRate(): Long {
        return torrentSessionManager.uploadRate()
    }

    fun getDownloadRate(): Long {
        return torrentSessionManager.downloadRate()
    }

    fun getDhtNodes(): Long {
        return torrentSessionManager.dhtNodes()
    }

    fun startDHT() {
        //TODO remove hardcoded udp DHT routers
        torrentSettingsPack.setString(
            settings_pack.string_types.dht_bootstrap_nodes.swigValue(),
            "router.silotis.us:6881"
        );
        torrentSettingsPack.setString(
            settings_pack.string_types.dht_bootstrap_nodes.swigValue(),
            "router.bittorrent.com:6881"
        );
        torrentSettingsPack.setString(
            settings_pack.string_types.dht_bootstrap_nodes.swigValue(),
            "dht.transmissionbt.com:6881"
        );
        torrentSettingsPack.enableDht()
        torrentSettingsPack.seedingOutgoingConnections(true)
        val params = SessionParams(torrentSettingsPack)

        torrentSessionManager.startDht()
        torrentSessionManager.start(params)
    }

    /**
     * Assume that the Uri given is a path to a local audio file. Create a torrent for this file
     * and start seeding the torrent.
     * @return magnet link for the torrent
     */
    fun seedFile(context: Context, uri: Uri): String {
        val torrentFile = generateTorrent(context, uri)
        //'Downloading' the file while already having it locally should start seeding it
        val torrentInfo = TorrentInfo(torrentFile)
        torrentSessionManager.download(torrentInfo, context.cacheDir)
        return torrentInfo.makeMagnetUri()
    }

    /**
     * Generates a a .torrent File
     * @param uri the URI of a single local source file to publish
     */
    @Throws(Resources.NotFoundException::class)
    private fun generateTorrent(context: Context, uri: Uri): File {
        println("Trying to share torrent $uri")
        val input = context.contentResolver.openInputStream(uri);

        //TODO generate a suitable signature for this torrent
        val hash = Random().nextInt().toString() + ".mp3"
        if (input == null) throw Resources.NotFoundException()
        val tempFileLocation = "${context.cacheDir}/$hash"

        //TODO currently creates temp copies before seeding, but should not be necessary
        Files.copy(input, Paths.get(tempFileLocation))
        val file = File(tempFileLocation)
        val torrent = SharedTorrent.create(file, 65535, listOf(), "")
        val torrentFile = "$tempFileLocation.torrent"
        torrent.save(FileOutputStream(torrentFile))
        return File(torrentFile)
    }

}
