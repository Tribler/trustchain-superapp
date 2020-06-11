package com.example.musicdao

import android.graphics.Color
import android.os.Bundle
import android.view.View
import androidx.core.os.bundleOf
import androidx.navigation.Navigation
import androidx.navigation.findNavController
import androidx.navigation.fragment.findNavController
import kotlinx.android.synthetic.main.fragment_base.*
import kotlinx.android.synthetic.main.fragment_release_cover.*
import nl.tudelft.ipv8.attestation.trustchain.TrustChainBlock
import nl.tudelft.ipv8.attestation.trustchain.TrustChainTransaction
import nl.tudelft.trustchain.common.ui.BaseFragment

/**
 * An album cover that can be clicked to view its contents
 */
class ReleaseCoverFragment(private val trustChainBlock: TrustChainBlock) :
    BaseFragment(R.layout.fragment_release_cover) {
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val transaction = trustChainBlock.transaction
        val publisher = transaction["publisher"].toString()
        val magnet = transaction["magnet"].toString()
        val title = transaction["title"].toString()
        val artists = transaction["artists"].toString()
        val date = transaction["date"].toString()

        coverTitle.text = title
        coverArtists.text = artists

        coverCard.setOnClickListener {
            val action =
                ReleaseOverviewFragmentDirections.actionReleaseOverviewFragmentToPlaylistFragment(publisher, magnet, title, artists, date)
            findNavController().navigate(action)
        }
    }
}
