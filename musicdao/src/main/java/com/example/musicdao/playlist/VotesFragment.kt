package com.example.musicdao.playlist

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
import androidx.viewpager2.widget.ViewPager2
import com.example.musicdao.R
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.ArrayList

class VotesFragment : Fragment() {

    private lateinit var adapter: Adapter
    private lateinit var viewPager: ViewPager2

    private val TAB_NAMES = arrayOf("Upvotes", "Downvotes", "Undecided votes")

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_votes, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        adapter = Adapter(this)
        viewPager = view.findViewById(R.id.viewpager)
        viewPager.adapter = adapter

        val tabLayout = view.findViewById<TabLayout>(R.id.tab_layout)
        TabLayoutMediator(tabLayout, viewPager) { tab, position ->
            tab.text = TAB_NAMES[position]
        }.attach()
    }
}

class Adapter(fragment: Fragment) : FragmentStateAdapter(fragment) {

    override fun getItemCount(): Int = 3

    override fun createFragment(position: Int): Fragment {
        // Return a NEW fragment instance in createFragment(int)
        val fragment = FragmentObject()
        fragment.arguments = Bundle().apply {
            putInt("tapPosition", position)
        }
        return fragment
    }
}

class FragmentObject : Fragment() {
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

            val votes = arrayListOf(
                "Cristiano Ronaldo",
                "Messi",
                "Neymar",
                "Isco",
                "Hazard",
                "Mbappe",
                "Hazard",
                "Ziyech",
                "Suarez"
            )

            val votesList = view.findViewById<ListView>(R.id.votes)
            val adapter = VotesAdapter(view.context, votes, requireArguments().getInt("tapPosition"))
            votesList.adapter = adapter
        }
    }
}

class VotesAdapter(private val context: Context, private val voters: ArrayList<String>, private val tabPosition: Int) :
    BaseAdapter() {

    // Remove this when actual data
    private var d = Date()

    override fun getCount(): Int {
        return voters.size
    }

    override fun getItem(position: Int): Any {
        return voters[position]
    }

    override fun getItemId(position: Int): Long {
        return position.toLong()
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
        val view =
            LayoutInflater.from(context).inflate(R.layout.fragment_votes_entry, parent, false)

        view.findViewById<TextView>(R.id.voter_name).text = voters[position]

        if (tabPosition != 2) {
            // Remove this when actual data
            d = Date(d.time - 100000000)
            val simpleDateFormat = SimpleDateFormat("dd-MM-yyyy HH:mm:ss")
            val formatted = simpleDateFormat.format(d)

            view.findViewById<TextView>(R.id.voter_time).text = formatted.toString()
        }

        return view
    }
}
