package com.example.musicdao

import android.content.Context
import android.os.Bundle
import android.view.View
import kotlinx.android.synthetic.main.fragment_playlist.*

class PlaylistFragment : MusicFragment(R.layout.fragment_playlist) {

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

            // TODO fix: Release is currently added every single time the fragment is being attached to the view,
            // Also the Release fragment leads to crashes
            if (magnet != null) {
                val transaction = childFragmentManager.beginTransaction()
                val release = Release(
                    magnet, artists, title, date, publisher, torrentInfoName
                )
                transaction.add(R.id.trackListLinearLayout, release, "release")
                transaction.commit()
            }
        }
    }
}
