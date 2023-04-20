package nl.tudelft.trustchain.detoks.fragments

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ListView
import android.widget.Toast
import androidx.activity.addCallback
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

        if (!adapter.isEmpty) adapter.clear()
        adapter.addAll(torrentManager.videoList.map { x -> Pair(x.fileName, community.getLikes(x.fileName, x.torrentName).size) })
        adapter.notifyDataSetChanged()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val reloadButton = view.findViewById<ImageButton>(R.id.reloadButton)
        reloadButton.setOnClickListener {
            update()
        }

        adapter = VideosListAdapter(requireActivity(), arrayListOf())
        update()

        val discoveryList = view.findViewById<ListView>(R.id.discoveryList)
        discoveryList.adapter = adapter
        discoveryList.setOnItemLongClickListener { _, _, i,_ ->
            val clipboardManager = requireActivity().getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val data = adapter.getItem(i)
            if (data != null) {
                val clipData = ClipData.newPlainText("text", data.first)
                clipboardManager.setPrimaryClip(clipData)
                Toast.makeText(this.requireContext(), "Text copied to clipboard.", Toast.LENGTH_LONG).show()
            }
            true
        }

        requireActivity().onBackPressedDispatcher.addCallback {
            view.findNavController().navigate(DiscoveryFragmentDirections.actionDiscoveryFragmentToTabBarFragment())
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_discovery, container, false)
    }
}
