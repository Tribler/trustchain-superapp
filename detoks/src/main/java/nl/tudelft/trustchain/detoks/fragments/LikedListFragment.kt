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
import nl.tudelft.trustchain.detoks.adapters.LikedListAdapter

class LikedListFragment(private val likedVideos: List<String>) : Fragment() {
    private lateinit var adapter: ArrayAdapter<String>

    fun updateLikedList() {
        val community = IPv8Android.getInstance().getOverlay<DeToksCommunity>()!!
        val author = community.myPeer.publicKey.toString()
        val likedVideos = community.listOfLikedVideosAndTorrents(author).map { it.first }

        adapter.clear()
        adapter.addAll(likedVideos)
        adapter.notifyDataSetChanged()
    }

    override fun onResume() {
        super.onResume()
        updateLikedList()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        adapter = LikedListAdapter(requireActivity(), likedVideos)
        val listView = view.findViewById<ListView>(R.id.listView)
        listView.adapter = adapter

        listView.setOnItemLongClickListener { _, _, i,_ ->
            val clipboardManager = requireActivity().getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val data = adapter.getItem(i)
            if (data != null) {
                val clipData = ClipData.newPlainText("text", data)
                clipboardManager.setPrimaryClip(clipData)
                Toast.makeText(this.requireContext(), "Text copied to clipboard.", Toast.LENGTH_LONG).show()
            }
            true
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_liked_list, container, false)
    }
}
