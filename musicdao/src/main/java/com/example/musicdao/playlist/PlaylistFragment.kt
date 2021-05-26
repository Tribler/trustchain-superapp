package com.example.musicdao.playlist

import android.os.Bundle
import com.example.musicdao.MusicBaseFragment
import com.example.musicdao.MusicService
import com.example.musicdao.R

/**
 * This is currently simply a container for one ReleaseFragment. It could be in the future altered
 * to contain tracks from different Releases
 */
class PlaylistFragment : MusicBaseFragment(R.layout.fragment_playlist) {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val localArgs = arguments
        if (localArgs is Bundle) {
            val magnet = localArgs.getString("magnet", "Magnet not found")
            val artists = localArgs.getString("artists", "Artists not found")
            val title = localArgs.getString("title", "Title not found")
            val date = localArgs.getString("date", "Date not found")
            val publisher = localArgs.getString("publisher", "Publisher not found")
            val torrentInfoName = localArgs.getString("torrentInfoName")

            val sessionManager = (activity as MusicService).sessionManager ?: return

            if (magnet != null) {
                val transaction = childFragmentManager.beginTransaction()
                val release = ReleaseFragment(
                    magnet,
                    artists,
                    title,
                    date,
                    publisher,
                    torrentInfoName,
                    sessionManager
                )
                transaction.add(R.id.trackListLinearLayout, release, "release")
                transaction.commit()
            }
        }
    }
}
