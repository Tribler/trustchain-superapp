package nl.tudelft.trustchain.detoks.fragments

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.ListView
import android.widget.Toast
import androidx.fragment.app.Fragment
import nl.tudelft.ipv8.android.IPv8Android
import nl.tudelft.trustchain.detoks.DeToksCommunity
import nl.tudelft.trustchain.detoks.R
import nl.tudelft.trustchain.detoks.adapters.VideosListAdapter

class VideosListFragment(private val likesPerVideo: List<Pair<String, Int>>) : Fragment() {
    private lateinit var adapter: ArrayAdapter<Pair<String, Int>>

    fun updateVideos() {
        if (this::adapter.isInitialized) {
            val community = IPv8Android.getInstance().getOverlay<DeToksCommunity>()!!
            val author = community.myPeer.publicKey.toString()
            val videos = community.getPostedVideos(author)

            adapter.clear()
            adapter.addAll(videos)
            adapter.notifyDataSetChanged()
        }
    }

    override fun onResume() {
        super.onResume()
        updateVideos()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        adapter = VideosListAdapter(requireActivity(), likesPerVideo)
        val listView = view.findViewById<ListView>(R.id.listView)
        listView.adapter = adapter

        listView.setOnItemLongClickListener { _, _, i,_ ->
            val clipboardManager = requireActivity().getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val data = adapter.getItem(i)
            if (data != null) {
                val clipData = ClipData.newPlainText("text", data.first)
                clipboardManager.setPrimaryClip(clipData)
                Toast.makeText(this.requireContext(), "Text copied to clipboard.", Toast.LENGTH_LONG).show()
            }
            true
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_videos_list, container, false)
    }
}
