package nl.tudelft.trustchain.detoks.fragments

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Context.CLIPBOARD_SERVICE
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ListView
import android.widget.Toast
import androidx.core.content.ContextCompat.getSystemService
import androidx.fragment.app.Fragment
import nl.tudelft.trustchain.detoks.R
import nl.tudelft.trustchain.detoks.adapters.VideosListAdapter

class VideosListFragment(private val likesPerVideo: List<Pair<String, Int>>) : Fragment() {
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val listView = view.findViewById<ListView>(R.id.listView)
        listView.adapter = VideosListAdapter(requireActivity(), likesPerVideo)


//        TODO: Add an event listener which plays the video on click
//        listView.setOnItemClickListener{ adapterView, view, position, id ->
//
//        }


    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_videos_list, container, false)
    }
}
