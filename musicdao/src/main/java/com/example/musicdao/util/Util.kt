package com.example.musicdao.util

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import com.frostwire.jlibtorrent.Priority
import com.frostwire.jlibtorrent.Sha1Hash
import com.frostwire.jlibtorrent.TorrentHandle
import com.frostwire.jlibtorrent.TorrentInfo
import com.mpatric.mp3agic.Mp3File
import java.io.File
import java.io.FileOutputStream

object Util {

    /**
     * Obtains the Display Name from a magnet link
     */
    fun extractNameFromMagnet(magnetLink: String): String {
        val substring = magnetLink.substringAfter("&dn=")
        return substring.substringBefore("&")
    }

    fun extractInfoHash(magnetLink: String): Sha1Hash? {
        val substring = magnetLink.substringAfter("urn:btih:")
        val infoHash = substring.substringBefore("&")
        if (infoHash.length > 39) return Sha1Hash(infoHash)
        return null
    }

    /**
     * Calculate how much percentage is downloaded of a certain track file based on its file size
     */
    fun calculateDownloadProgress(fileProgress: Long, fullSize: Long?): Int {
        val size = fullSize ?: Long.MAX_VALUE
        val progress: Double = (fileProgress.toDouble() / size.toDouble()) * 100.0
        return progress.toInt()
    }

    fun calculatePieceIndex(fileIndex: Int, torrentInfo: TorrentInfo): Int {
        var pieceIndex = 0
        for (i in 0..fileIndex) {
            var fullSize = torrentInfo.files().fileSize(i)
            while (fullSize > 1) {
                fullSize -= torrentInfo.pieceSize(pieceIndex)
                pieceIndex += 1
            }
        }
        return pieceIndex
    }

    /**
     * Converts bytes into a string that shows it in a readable format
     */
    fun readableBytes(bytes: Long): String {
        if (bytes > 1024 && bytes <= (1024 * 1024)) return "${(bytes / 1024)}Kb"
        if (bytes > (1024 * 1024)) return "${(bytes / (1024 * 1024))}Mb"
        return "${bytes}B"
    }

    /**
     * Prefer the first pieces to be downloaded of the selected audio file over the other pieces,
     * so that the first seconds of the track can be buffered as soon as possible
     */
    fun setTorrentPriorities(
        torrentHandle: TorrentHandle,
        onlyCalculating: Boolean = false,
        pieceIndex: Int = 0,
        fileIndex: Int = 0
    ): Array<Priority> {
        var interestedPieceIndex = pieceIndex
        if (interestedPieceIndex == -1) interestedPieceIndex = 0
        val piecePriorities: Array<Priority> =
            torrentHandle.piecePriorities()
        // Set the 5 concurrent pieces of the interested portion of some track as high priority
        for (i in interestedPieceIndex until interestedPieceIndex + 5) {
            if (piecePriorities.indices.contains(i)) {
                piecePriorities[i] = Priority.SIX
            }
        }
        if (onlyCalculating) return piecePriorities // For making unit test possible
        for ((index, priority) in piecePriorities.withIndex()) {
            torrentHandle.piecePriority(index, priority)
        }
        // Set file priority high of the currently selected track
        if (torrentHandle.filePriorities().indices.contains(fileIndex)) {
            torrentHandle.filePriority(fileIndex, Priority.SIX)
        }
        return piecePriorities
    }

    fun checkAndSanitizeTrackNames(fileName: String): String? {
        var fileNameLocal = fileName
        val allowedExtensions =
            listOf(".flac", ".mp3", ".3gp", ".aac", ".mkv", ".wav", ".ogg", ".mp4", ".m4a")
        for (s in allowedExtensions) {
            if (fileNameLocal.endsWith(s)) {
                fileNameLocal = fileNameLocal.substringBefore(s)
                fileNameLocal = fileNameLocal.replace("_", " ")
                return fileNameLocal
            }
        }
        return null
    }

    /**
     * Checks if a music file is valid, and make it more readable if it is
     */
    fun getTitle(mp3File: Mp3File): String? {
        try {
            if (mp3File.hasId3v2Tag()) {
                return mp3File.id3v2Tag.title
            }
            if (mp3File.hasId3v1Tag()) {
                return mp3File.id3v1Tag.title
            }
        } catch (e: Exception) {}
        return null
    }

    /**
     * Check if a torrent file has all its corresponding files downloaded
     */
    fun isTorrentCompleted(torrentInfo: TorrentInfo, saveDirectory: File): Boolean {
        val dir = File(saveDirectory.path + "/" + torrentInfo.name())
        if (!dir.isDirectory) return false
        if (folderSize(dir) != torrentInfo.totalSize()) return false
        return true
    }

    private fun folderSize(dir: File): Long {
        var length = 0.toLong()
        if (!dir.isDirectory) return 0
        val files = dir.listFiles() ?: return 0
        for (file in files) {
            length += if (file.isFile) {
                file.length()
            } else {
                folderSize(file)
            }
        }
        return length
    }

    /**
     * Go through a directory that contains a musical Release and find an image that corresponds to
     * the cover art
     * @param directory the directory containing music files, possibly cover art and possibly other
     * files
     */
    fun findCoverArt(directory: File): File? {
        if (!directory.isDirectory) return null
        val allowedExtensions =
            listOf(".jpg", ".png")
        val files = directory.listFiles() ?: return null
        // Give priority to files named "Cover"
        for (ext in allowedExtensions) {
            val cover = File(directory.path + "/" + "cover" + ext)
            if (cover.isFile) {
                return cover
            }
        }
        // If no file named "Cover" exists, we try to extract the albumImage from the first MP3 we
        // see, and assume it is the album image for the whole Release
        for (file in files) {
            try {
                val mp3File = Mp3File(file)
                if (!mp3File.hasId3v2Tag()) continue
                val imageBytes = mp3File.id3v2Tag.albumImage
                val bmp = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
                // Once a valid BMP is found, save this bitmap to a PNG file named cover.png
                val coverFile = File(directory.path + "/cover.png")
                val fOut = FileOutputStream(coverFile)
                bmp.compress(Bitmap.CompressFormat.PNG, 85, fOut)
                fOut.flush()
                fOut.close()
                return coverFile
            } catch (e: Exception) {}
        }
        for (file in files) {
            for (ext in allowedExtensions) {
                if (file.name.endsWith(ext)) {
                    return file
                }
            }
        }
        return null
    }

    fun sanitizeString(input: String): String {
        return input.replace("%20", " ")
    }
}
