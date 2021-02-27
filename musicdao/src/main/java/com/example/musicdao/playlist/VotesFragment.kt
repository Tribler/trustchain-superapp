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
import org.json.JSONObject
import java.text.SimpleDateFormat


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

        val votersString =
            this.context?.assets?.open("temp_voters.json")?.bufferedReader().use { it?.readText() }
        val voters = Response(votersString.toString())


        adapter = Adapter(this, voters)
        viewPager = view.findViewById(R.id.viewpager)
        viewPager.adapter = adapter

        var favorVoters = 0
        var againstVoters = 0
        var undecidedVotes = 0

        for (voter in voters.data!!) {
            when (voter.voted) {
                0 -> favorVoters++
                1 -> againstVoters++
                2 -> undecidedVotes++
            }
        }

        val localArgs = arguments
        if (localArgs is Bundle) {
            val title = localArgs.getString("title", "Title not found")
            val price = "50mBTC"
            val userHasVoted = false

            cover_title.text = title
            vote_tip_price.text = getString(R.string.bounty_payout, price, title)
            fab.setOnClickListener { v ->
                val builder = AlertDialog.Builder(v.context)
                builder.setTitle(getString(R.string.bounty_payout, price, title))
                builder.setMessage(
                    getString(R.string.bounty_payout_message, price, title, favorVoters, againstVoters, undecidedVotes)
                )
                builder.setPositiveButton("YES") { _, _ ->
                    Toast.makeText(
                        v.context,
                        getString(R.string.bounty_payout_upvoted, price, title), Toast.LENGTH_SHORT
                    ).show()
                    userHasAlreadyVoted()
                }

                builder.setNeutralButton("NO") { _, _ ->
                    Toast.makeText(
                        v.context,
                        getString(R.string.bounty_payout_downvoted, price, title),
                        Toast.LENGTH_SHORT
                    ).show()
                    userHasAlreadyVoted()
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
        vote_tip_price.visibility = View.GONE
        fab.visibility = View.GONE
    }
}

class Adapter(fragment: Fragment, voters: Response) : FragmentStateAdapter(fragment) {

    private var voters = voters.data
    override fun getItemCount(): Int = 3

    override fun createFragment(position: Int): Fragment {
        // Return a NEW fragment instance in createFragment(int)
        val fragment = FragmentObject(voters)
        fragment.arguments = Bundle().apply {
            putInt("tapPosition", position)
        }
        return fragment
    }
}

class FragmentObject(private var voters: List<Voter>?) : Fragment() {
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
            val newVoters = voters?.filter { v -> v.voted == getInt("tapPosition") }
            val adapter =
                VotesAdapter(view.context, newVoters, requireArguments().getInt("tapPosition"))
            votesList.adapter = adapter
        }
    }
}

class VotesAdapter(
    private val context: Context,
    private val voters: List<Voter>?,
    private val tabPosition: Int
) :
    BaseAdapter() {

    override fun getCount(): Int {
        return voters!!.size
    }

    override fun getItem(position: Int): Any {
        return voters?.get(position)!!
    }

    override fun getItemId(position: Int): Long {
        return position.toLong()
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
        val view =
            LayoutInflater.from(context).inflate(R.layout.fragment_votes_entry, parent, false)

        val voter = voters!![position]
        view.findViewById<TextView>(R.id.voter_name).text = voter.name

        if (tabPosition != 2) {
            // Remove this when actual data
            val simpleDateFormat = SimpleDateFormat("dd-MM-yyyy HH:mm:ss")
            val formatted = simpleDateFormat.format(voter.timestamp)

            view.findViewById<TextView>(R.id.voter_time).text = formatted.toString()
        }

        return view
    }
}

class Response(json: String) : JSONObject(json) {
    val data = this.optJSONArray("data")
        ?.let {
            0.until(it.length()).map { i -> it.optJSONObject(i) }
        } // returns an array of JSONObject
        ?.map { Voter(it.toString()) } // transforms each JSONObject of the array into Foo
}

class Voter(json: String) : JSONObject(json) {
    val name: String = this.optString("name")
    val voted = this.optInt("voted")
    val timestamp = this.optInt("timestamp")
}
