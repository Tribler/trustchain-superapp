package com.example.musicdao

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import kotlinx.android.synthetic.main.fragment_release_cover.*
import nl.tudelft.trustchain.common.ui.BaseFragment

class ReleaseCoverFragment : BaseFragment(R.layout.fragment_release_cover) {
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        coverTitle.text = "Random Album Title"
        coverArtists.text = "Deadmau5"

        coverCard.setOnClickListener {
            val action =
                ReleaseOverviewFragmentDirections.actionReleaseOverviewFragmentToMusicServiceFragment()
            findNavController().navigate(action)
        }
    }
}
