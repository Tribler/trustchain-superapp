package nl.tudelft.trustchain.musicdao.core.util

import com.frostwire.jlibtorrent.Priority
import com.frostwire.jlibtorrent.TorrentHandle
import com.mpatric.mp3agic.Mp3File

object Util {

    /**
     * Calculate how much percentage is downloaded of a certain track file based on its file size
     */
    fun calculateDownloadProgress(fileProgress: Long, fullSize: Long?): Int {
        val size = fullSize ?: Long.MAX_VALUE
        val progress: Double = (fileProgress.toDouble() / size.toDouble()) * 100.0
        return progress.toInt()
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

        // Attempt to set the last piece of the selected file as a high priority
        val pieceSize = torrentHandle.torrentFile().pieceLength()
        val fileSize = torrentHandle.torrentFile().files().fileSize(fileIndex)
        val offset = torrentHandle.torrentFile().files().fileOffset(fileIndex)

        val finalPieceBytes = offset + fileSize - 1
        if (finalPieceBytes > 0) {
            val finalPieceIndex = finalPieceBytes / pieceSize
            if (piecePriorities.indices.contains(finalPieceIndex.toInt())) {
                piecePriorities[finalPieceIndex.toInt()] = Priority.SIX
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
                fileNameLocal = fileNameLocal.substringAfterLast("-")
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
        } catch (e: Exception) {
            return null
        }
        return null
    }
}
