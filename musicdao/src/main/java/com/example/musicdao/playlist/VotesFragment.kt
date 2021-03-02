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
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import com.example.musicdao.R
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import kotlinx.android.synthetic.main.fragment_votes.*
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.ArrayList


class VotesFragment : Fragment() {

    private lateinit var adapter: Adapter
    private lateinit var viewPager: ViewPager2

    private val TAB_NAMES = arrayOf("Upvotes", "Downvotes", "Undecided votes")

    // initialize voters with 0 pro, 0 against and 2 undecided votes
    private val voters =
        mutableMapOf(0 to arrayListOf(), 1 to arrayListOf(), 2 to arrayListOf("Rick", "Steven"))

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_votes, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {

        adapter = Adapter(this, voters)
        viewPager = view.findViewById(R.id.viewpager)
        viewPager.adapter = adapter

        val localArgs = arguments
        if (localArgs is Bundle) {
            val title = localArgs.getString("artists", "Artists not found")
            val price = localArgs.getString("amount", "Price not found") + "BTC"
            val userHasVoted = false

            cover_title.text = title
            vote_tip_price.text = getString(R.string.bounty_payout, price, title)
            fab_user.setOnClickListener { v ->
                val builder = AlertDialog.Builder(v.context)
                builder.setTitle(getString(R.string.bounty_payout, price, title))
                builder.setMessage(getString(R.string.bounty_payout_message,price,title,1,2,3))
                builder.setPositiveButton("YES") { _, _ ->
                    Toast.makeText(v.context,getString(R.string.bounty_payout_upvoted, price, title), Toast.LENGTH_SHORT).show()
                    voters[0]!!.add("Rick")
                    voters[2]!!.remove("Rick")
                    userHasAlreadyVoted()
                    adapter.notifyChanges()
                }

                builder.setNeutralButton("NO") { _, _ ->
                    Toast.makeText(v.context,getString(R.string.bounty_payout_downvoted, price, title),Toast.LENGTH_SHORT).show()
                    voters[1]!!.add("Rick")
                    voters[2]!!.remove("Rick")
                    userHasAlreadyVoted()
                    adapter.notifyChanges()
                }
                builder.show()
            }

            // FOR THE DEMO TOMORROW
            fab_demo.setOnClickListener { v ->
                val builder = AlertDialog.Builder(v.context)
                builder.setTitle(getString(R.string.bounty_payout, price, title))
                builder.setMessage(getString(R.string.bounty_payout_message,price,title,1,2,3))
                builder.setPositiveButton("YES") { _, _ ->
                    Toast.makeText(v.context,getString(R.string.bounty_payout_upvoted, price, title), Toast.LENGTH_SHORT).show()
                    voters[0]!!.add("Steven")
                    voters[2]!!.remove("Steven")
                    fab_demo.visibility = View.GONE
                    adapter.notifyChanges()
                }

                builder.setNeutralButton("NO") { _, _ ->
                    Toast.makeText(v.context,getString(R.string.bounty_payout_downvoted, price, title),Toast.LENGTH_SHORT).show()
                    voters[1]!!.add("Steven")
                    voters[2]!!.remove("Steven")
                    fab_demo.visibility = View.GONE
                    adapter.notifyChanges()
                }
                builder.show()
            }

            if (userHasVoted) {
                userHasAlreadyVoted()
            }
        }

        val tabLayout = view.findViewById<TabLayout>(R.id.tab_layout)
        TabLayoutMediator(tabLayout, viewPager) { tab, position ->
            tab.text = TAB_NAMES[position]
        }.attach()
    }

    private fun userHasAlreadyVoted() {
        fab_user.visibility = View.GONE
    }
}

class Adapter(fragment: Fragment, private val voters: Map<Int, ArrayList<String>>) :
    FragmentStateAdapter(fragment) {

    private var fragmentList = arrayListOf<FragmentObject>()

    override fun getItemCount(): Int = 3

    override fun createFragment(position: Int): Fragment {
        // Return a NEW fragment instance in createFragment(int)
        val fragment = FragmentObject()
        fragmentList.add(fragment)
        fragment.arguments = Bundle().apply {
            putInt("tapPosition", position)
            putStringArrayList("voters", voters[position])
        }
        return fragment
    }

    // Somehow notifying for changes doesn't work properly..
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
            adapter = VotesAdapter(view.context,requireArguments().getStringArrayList("voters"),requireArguments().getInt("tapPosition"))
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
            val simpleDateFormat = SimpleDateFormat("dd-MM-yyyy HH:mm:ss")
            val formatted = simpleDateFormat.format(Date())

            view.findViewById<TextView>(R.id.voter_time).text = formatted.toString()
        }

        return view
    }
}
