package com.example.musicdao.core.util

import android.util.Log

class TorrentUtil {

    companion object {
        fun magnetToInfoHash(magnet: String): String? {
            val mark = "magnet:?xt=urn:btih:"
            val start = magnet.indexOf(mark) + mark.length
            Log.d("MusicDao", "$magnet, $mark, $start, ${start + 40}")
            if (start == -1) return null
            return magnet.substring(start, start + 40)
        }
    }
}
