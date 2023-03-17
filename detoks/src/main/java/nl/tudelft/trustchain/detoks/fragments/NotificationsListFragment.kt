package nl.tudelft.trustchain.detoks.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ListView
import androidx.fragment.app.Fragment
import nl.tudelft.trustchain.detoks.R
import nl.tudelft.trustchain.detoks.adapters.NotificationsListAdapter

class NotificationsListFragment(private val notifications: List<String>) : Fragment() {
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val listView = view.findViewById<ListView>(R.id.listView)
        listView.adapter = NotificationsListAdapter(requireActivity(), notifications)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_notifications_list, container, false)
    }
}
