package com.example.musicdao.util

import com.frostwire.jlibtorrent.Priority
import com.frostwire.jlibtorrent.Sha1Hash
import com.frostwire.jlibtorrent.TorrentInfo
import com.github.se_bastiaan.torrentstream.Torrent
import com.mpatric.mp3agic.Mp3File
import java.io.File

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
    fun setSequentialPriorities(torrent: Torrent, onlyCalculating: Boolean = false): Array<Priority> {
        val piecePriorities: Array<Priority> =
            torrent.torrentHandle.piecePriorities()
        for ((index, piecePriority) in piecePriorities.withIndex()) {
            if (piecePriority == Priority.SEVEN) {
                piecePriorities[index] = Priority.SIX
            }
            if (piecePriority == Priority.NORMAL) {
                piecePriorities[index] = Priority.FIVE
            }
            if (piecePriority == Priority.IGNORE) {
                piecePriorities[index] = Priority.NORMAL
            }
        }
        for (i in torrent.interestedPieceIndex until torrent.interestedPieceIndex + torrent.piecesToPrepare) {
            piecePriorities[i] = Priority.SEVEN
        }
        if (onlyCalculating) return piecePriorities // For making unit test possible
        for ((index, priority) in piecePriorities.withIndex()) {
            torrent.torrentHandle.piecePriority(index, priority)
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
