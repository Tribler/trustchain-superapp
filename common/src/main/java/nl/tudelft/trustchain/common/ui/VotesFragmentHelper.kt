package nl.tudelft.trustchain.common.ui

import android.content.Context
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.ListView
import android.widget.TextView
import androidx.annotation.RequiresApi
import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import nl.tudelft.trustchain.common.R
import java.text.SimpleDateFormat
import java.util.*

class Adapter(fragment: Fragment, private val voters: Map<Int, ArrayList<String>>) :
    FragmentStateAdapter(fragment) {

    private var fragmentList = arrayListOf<FragmentObject>()

    override fun getItemCount(): Int = 3

    override fun createFragment(position: Int): Fragment {
        // Return a NEW fragment instance in createFragment(int)
        val fragment = FragmentObject()
        fragment.arguments = Bundle().apply {
            putInt("tapPosition", position)
            putStringArrayList("voters", voters[position])
        }
        fragmentList.add(fragment)
        return fragment
    }

    // TODO Somehow notifying for changes doesn't work properly..
    fun notifyChanges() {
        for (fragment in fragmentList) {
            fragment.adapter.notifyDataSetChanged()
        }
        this.notifyDataSetChanged()
    }
}

class FragmentObject : Fragment() {

    lateinit var adapter: VotesAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.fragment_votes_tab, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        arguments?.takeIf {
            it.containsKey("tapPosition")
        }?.apply {

            val votesList = view.findViewById<ListView>(R.id.votes)
            adapter = VotesAdapter(
                view.context,
                requireArguments().getStringArrayList("voters"),
                requireArguments().getInt(
                    "tapPosition"
                )
            )
            votesList.adapter = adapter
        }
    }
}

class VotesAdapter(
    private val context: Context,
    private val voters: ArrayList<String>?,
    private val tabPosition: Int
) :
    BaseAdapter() {

    override fun getCount(): Int {
        return voters!!.size
    }

    override fun getItem(position: Int): Any {
        return voters!![position]
    }

    override fun getItemId(position: Int): Long {
        return position.toLong()
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
        val view =
            LayoutInflater.from(context).inflate(R.layout.fragment_votes_entry, parent, false)

        val voter = voters!![position]
        view.findViewById<TextView>(R.id.voter_name).text = voter

        if (tabPosition != 2) {
            // Remove this when actual data
            val simpleDateFormat = SimpleDateFormat("dd-MM-yyyy HH:mm:ss", Locale.getDefault())
            val formatted = simpleDateFormat.format(Date())

            view.findViewById<TextView>(R.id.voter_time).text = formatted.toString()
        }

        return view
    }
}
