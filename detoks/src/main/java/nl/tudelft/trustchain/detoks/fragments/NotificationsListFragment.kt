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
import nl.tudelft.trustchain.detoks.DetoksCommunity
import nl.tudelft.trustchain.detoks.R
import nl.tudelft.trustchain.detoks.adapters.NotificationsListAdapter

class NotificationsListFragment(private val notifications: List<String>) : Fragment() {
    private lateinit var adapter: ArrayAdapter<String>

    fun updateNotifications() {
        val community = IPv8Android.getInstance().getOverlay<DetoksCommunity>()!!
        val author = community.myPeer.publicKey.toString()
        val notifications = community.getBlocksByAuthor(author).map { "Received a like: " + it.transaction["video"] }

        adapter.clear()
        adapter.addAll(notifications)
        adapter.notifyDataSetChanged()
    }

    override fun onResume() {
        super.onResume()
        updateNotifications()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        adapter = NotificationsListAdapter(requireActivity(), notifications)
        val listView = view.findViewById<ListView>(R.id.listView)
        listView.adapter = adapter

//        TODO: Add an event listener which plays the video on click
//        listView.setOnItemClickListener{ adapterView, view, position, id ->
//
//        }

        listView.setOnItemLongClickListener { _, _, i,_ ->
            val clipboardManager = requireActivity().getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val data = adapter.getItem(i)
            if (data != null) {
                val clipData = ClipData.newPlainText("text", data.substring(data.indexOf(':') + 2))
                clipboardManager.setPrimaryClip(clipData)
                Toast.makeText(this.requireContext(), "Text copied to clipboard.", Toast.LENGTH_LONG).show()
            }
            true
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_notifications_list, container, false)
    }
}
