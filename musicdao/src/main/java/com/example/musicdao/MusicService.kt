package com.example.musicdao

import android.content.Context
import android.content.res.Resources
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.view.ContextMenu
import android.view.View
import android.widget.Toast
import com.frostwire.jlibtorrent.SessionManager
import com.frostwire.jlibtorrent.TorrentInfo
import com.github.se_bastiaan.torrentstream.TorrentOptions
import com.github.se_bastiaan.torrentstream.TorrentStream
import com.turn.ttorrent.client.SharedTorrent
import nl.tudelft.ipv8.android.IPv8Android
import nl.tudelft.ipv8.attestation.trustchain.BlockSigner
import nl.tudelft.ipv8.attestation.trustchain.TrustChainBlock
import nl.tudelft.ipv8.attestation.trustchain.TrustChainCommunity
import nl.tudelft.trustchain.common.BaseActivity
import org.apache.commons.io.FileUtils
import java.io.*

class MusicService : BaseActivity() {
    lateinit var torrentStream: TorrentStream
    override val navigationGraph = R.navigation.musicdao_navgraph
    lateinit var contentSeeder: ContentSeeder

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        torrentStream = TorrentStream.init(
            TorrentOptions.Builder()
                .saveLocation(applicationContext.cacheDir)
                .removeFilesAfterStop(false)
                .autoDownload(false)
                .build())

        registerBlockSigner()

        contentSeeder = ContentSeeder(SessionManager(), applicationContext.cacheDir)
        contentSeeder.start()
    }

    override fun onCreateContextMenu(
        menu: ContextMenu?,
        v: View?,
        menuInfo: ContextMenu.ContextMenuInfo?
    ) {
        super.onCreateContextMenu(menu, v, menuInfo)
        menuInflater.inflate(R.menu.menu_add_playlist, menu)
    }

    fun getDhtNodes(): Int {
        return torrentStream.totalDhtNodes
    }

    /**
     * Clear cache on every run (for testing, and audio files may be large 15MB+). May be removed
     * in the future
     */
    private fun clearCache() {
        if (cacheDir.isDirectory && cacheDir.listFiles() != null) {
            val files = cacheDir.listFiles()
            files?.forEach {
                it.deleteRecursively()
            }
        }
    }

    /**
     * On discovering a half block, with tag publish_release, agree it immediately (for now). In the
     * future there will be logic added here to determine whether an upload was done by the correct
     * artist/label (artist passport).
     */
    private fun registerBlockSigner() {
        val trustchain = IPv8Android.getInstance().getOverlay<TrustChainCommunity>()!!
        trustchain.registerBlockSigner("publish_release", object : BlockSigner {
            override fun onSignatureRequest(block: TrustChainBlock) {
                Toast.makeText(
                    applicationContext,
                    "Signing block ${block.blockId}",
                    Toast.LENGTH_LONG
                ).show()
                trustchain.createAgreementBlock(block, mapOf<Any?, Any?>())
            }
        })
    }

    /**
     * Assume that the Uri given is a path to a local audio file. Create a torrent for this file
     * and start seeding the torrent.
     * @return magnet link for the torrent
     */
    fun seedFile(context: Context, uri: Uri): TorrentInfo {
        val torrentFile = generateTorrent(context, uri)
        // 'Downloading' the file while already having it locally should start seeding it
        return TorrentInfo(torrentFile)
    }

    /**
     * Generates a a .torrent File
     * @param uri the URI of a single local source file to publish
     */
    @Throws(Resources.NotFoundException::class)
    private fun generateTorrent(context: Context, uri: Uri): File {
        println("Trying to share torrent $uri")
        val contentResolver = context.contentResolver
        val projection =
            arrayOf<String>(MediaStore.MediaColumns.DISPLAY_NAME)
        val cursor = contentResolver.query(uri, projection, null, null, null)
        var fileName = ""
        if (cursor != null) {
            try {
                if (cursor.moveToFirst()) {
                    fileName = cursor.getString(0)
                }
            } finally {
                cursor.close()
            }
        }

        if (fileName == "") throw Error("Source file name for creating torrent not found")
        val input = contentResolver.openInputStream(uri) ?: throw Resources.NotFoundException()
        val tempFileLocation = "${context.cacheDir}/$fileName"

        // TODO currently creates temp copies before seeding, but should not be necessary
        FileUtils.copyInputStreamToFile(input, File(tempFileLocation))
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
