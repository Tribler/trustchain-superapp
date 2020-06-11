package com.example.musicdao

import android.content.Context
import android.content.res.Resources
import android.net.Uri
import android.provider.Settings
import com.example.musicdao.util.Util
import com.frostwire.jlibtorrent.*
import com.frostwire.jlibtorrent.swig.settings_pack
import com.turn.ttorrent.client.SharedTorrent
import java.io.*
import java.util.*

class TrackLibrary {
    private val torrentSessionManager = SessionManager()
    private val torrentSettingsPack = SettingsPack()

    fun getUploadRate(): String {
        return Util.readableBytes(torrentSessionManager.uploadRate())
    }

    fun getDownloadRate(): String {
        return Util.readableBytes(torrentSessionManager.downloadRate())
    }

    fun getDhtNodes(): Long {
        return torrentSessionManager.dhtNodes()
    }

//    fun downloadMagnet(release: Release, magnet: String, saveDir: File) {
//        torrentSessionManager.addListener(release)
//        torrentSessionManager.download(magnet, saveDir)
//    }

    fun startDHT() {
        // TODO remove hardcoded udp DHT routers
        torrentSettingsPack.setString(
            settings_pack.string_types.dht_bootstrap_nodes.swigValue(),
            "router.silotis.us:6881"
        )
        torrentSettingsPack.setString(
            settings_pack.string_types.dht_bootstrap_nodes.swigValue(),
            "router.bittorrent.com:6881"
        )
        torrentSettingsPack.setString(
            settings_pack.string_types.dht_bootstrap_nodes.swigValue(),
            "dht.transmissionbt.com:6881"
        )
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
        // 'Downloading' the file while already having it locally should start seeding it
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
        val input = context.contentResolver.openInputStream(uri)

        // TODO generate a suitable signature for this torrent
        val hash = Random().nextInt().toString() + ".mp3"
        if (input == null) throw Resources.NotFoundException()
        val tempFileLocation = "${context.cacheDir}/$hash"

        // TODO currently creates temp copies before seeding, but should not be necessary
        copyInputStreamToFile(input, File(tempFileLocation))
        val file = File(tempFileLocation)
        val torrent = SharedTorrent.create(file, 65535, listOf(), "")
        val torrentFile = "$tempFileLocation.torrent"
        torrent.save(FileOutputStream(torrentFile))
        return File(torrentFile)
    }

    /**
     * (External) helper method
     */
    private fun copyInputStreamToFile(inputStream: InputStream, file: File) {
        var out: OutputStream? = null
        try {
            out = FileOutputStream(file)
            val buf = ByteArray(1024)
            var len: Int
            while (inputStream.read(buf).also { len = it } > 0) {
                out.write(buf, 0, len)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            try {
                out?.close()
                inputStream.close()
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }
    }
}
