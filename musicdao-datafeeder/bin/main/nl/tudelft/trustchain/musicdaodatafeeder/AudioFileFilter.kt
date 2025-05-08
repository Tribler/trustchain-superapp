package nl.tudelft.trustchain.musicdaodatafeeder

import java.io.File
import java.io.FilenameFilter

class AudioFileFilter : FilenameFilter {
    override fun accept(dir: File?, name: String?): Boolean {
        if (name != null && name.endsWith(".mp3")) {
            return true
        }
        return false
    }
}
