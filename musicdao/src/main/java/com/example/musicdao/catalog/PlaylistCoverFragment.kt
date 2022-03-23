package com.example.musicdao.catalog

import android.net.Uri
import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.musicdao.R
import kotlinx.android.synthetic.main.fragment_release_cover.*
import nl.tudelft.ipv8.attestation.trustchain.TrustChainBlock
import java.io.File
import java.util.*

/**
 * An 'album cover' or other visual display of a playlist, that can be clicked to view its contents
 */
class PlaylistCoverFragment(
    private val trustChainBlock: TrustChainBlock,
    private val connectivity: Int = 0,
    private val coverArt: File? = null
) :
    Fragment(R.layout.fragment_release_cover) {
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val transaction = trustChainBlock.transaction
        val publisher = transaction["publisher"].toString()
        val magnet = transaction["magnet"].toString()
        val title = transaction["title"].toString()
        val artists = transaction["artists"].toString()
        val date = transaction["date"].toString()
        var torrentInfoName = ""
        if (transaction.containsKey("torrentInfoName")) {
            torrentInfoName = transaction["torrentInfoName"].toString()
        }

        coverTitle.text = title
        coverArtists.text = artists
        seedCount.text = "Peers: $connectivity"
        if (coverArt != null) {
            coverArtImage.setImageURI(Uri.fromFile(coverArt))
        }

        coverCard.setOnClickListener {
            val action =
                PlaylistsOverviewFragmentDirections.actionPlaylistsOverviewFragmentToPlaylistFragment(
                    publisher,
                    magnet,
                    title,
                    artists,
                    date,
                    torrentInfoName
                )
            findNavController().navigate(action)
        }
    }

    /**
     * Filter out the content by a simple string keyword
     */
    fun filter(name: String): Boolean {
        if (name.isEmpty()) return true
        val query = name.lowercase(Locale.ROOT)
        val transaction = trustChainBlock.transaction
        val title = transaction["title"].toString().lowercase(Locale.ROOT)
        val artists = transaction["artists"].toString().lowercase(Locale.ROOT)
        val date = transaction["date"].toString().lowercase(Locale.ROOT)
        if (title.contains(query)) {
            return true
        }
        if (artists.contains(query)) {
            return true
        }
        if (date.contains(query)) {
            return true
        }
        return false
    }
}
