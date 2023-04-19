package nl.tudelft.trustchain.detoks.fragments

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ListView
import androidx.navigation.findNavController
import nl.tudelft.ipv8.android.IPv8Android
import nl.tudelft.trustchain.detoks.DeToksCommunity
import nl.tudelft.trustchain.detoks.R
import nl.tudelft.trustchain.detoks.TorrentManager
import nl.tudelft.trustchain.detoks.adapters.VideosListAdapter

class DiscoveryFragment : Fragment() {
    lateinit var adapter: VideosListAdapter

    private fun update() {
        val torrentManager = TorrentManager.getInstance(requireContext())
        val community = IPv8Android.getInstance().getOverlay<DeToksCommunity>()!!

        adapter.clear()
        adapter.addAll(torrentManager.videoList.map { x -> Pair(x.fileName, community.getLikes(x.fileName, x.torrentName).size) })
        adapter.notifyDataSetChanged()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val backButton = view.findViewById<ImageButton>(R.id.backButton)
        val reloadButton = view.findViewById<ImageButton>(R.id.reloadButton)
        val discoveryList = view.findViewById<ListView>(R.id.discoveryList)

        adapter = VideosListAdapter(requireActivity(), arrayListOf())
        update()
        discoveryList.adapter = adapter

        backButton.setOnClickListener {
            it.findNavController().navigate(DiscoveryFragmentDirections.actionDiscoveryFragmentToTabBarFragment())
        }

        reloadButton.setOnClickListener {
            update()
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_discovery, container, false)
    }
}
