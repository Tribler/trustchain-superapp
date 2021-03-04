package nl.tudelft.trustchain.currencyii.ui.bitcoin

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
import androidx.navigation.fragment.findNavController
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import kotlinx.android.synthetic.main.fragment_votes.*
import nl.tudelft.trustchain.currencyii.R
import nl.tudelft.trustchain.currencyii.ui.BaseFragment
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.ArrayList

class VotesFragment : BaseFragment(R.layout.fragment_votes) {
    private lateinit var adapter: Adapter
    private lateinit var viewPager: ViewPager2

    private val TAB_NAMES = arrayOf("Upvotes", "Downvotes", "Undecided votes")

    // initialize voters with 0 pro, 0 against and 2 undecided votes
    //TODO call countVotes from api.
    private val voters =
        mutableMapOf(0 to arrayListOf(), 1 to arrayListOf(), 2 to arrayListOf("user1", "user2"))

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
            val walletId = localArgs.getString("wallet_id", "Wallet name not found")
            val priceString = localArgs.getString("amount", "Price not found") + "BTC"
            val userHasVoted = false

            wallet_id.text = walletId
            price.text = getString(R.string.bounty_payout, priceString, walletId)
            fab_user.setOnClickListener { v ->
                val builder = AlertDialog.Builder(v.context)
                builder.setTitle(getString(R.string.bounty_payout, priceString, walletId))
                builder.setMessage(
                    getString(
                        R.string.bounty_payout_message,
                        priceString,
                        walletId,
                        voters[0]!!.size,
                        voters[1]!!.size,
                        voters[2]!!.size
                    )
                )
                builder.setPositiveButton("YES") { _, _ ->
                    Toast.makeText(
                        v.context,
                        getString(R.string.bounty_payout_upvoted, priceString, walletId),
                        Toast.LENGTH_SHORT
                    ).show()
                    voters[0]!!.add("user1")
                    voters[2]!!.remove("user1")
                    userHasAlreadyVoted()
                    adapter.notifyChanges()

//                    val walletDir = context?.cacheDir ?: throw Error("CacheDir not found")
//                    val walletService =
//                        WalletService.getInstance(walletDir, (activity as MusicService))
//                    val result = walletService.signUser1()
//                    if (result) {
                    if (voters[2]!!.size == 0) {
                        findNavController().navigateUp()
                    }
                    // TODO: send yes vote for user1
                }

                builder.setNeutralButton("NO") { _, _ ->
                    Toast.makeText(
                        v.context, getString(R.string.bounty_payout_downvoted, priceString, walletId),
                        Toast.LENGTH_SHORT
                    ).show()
                    voters[1]!!.add("user1")
                    voters[2]!!.remove("user1")
                    userHasAlreadyVoted()
                    adapter.notifyChanges()

                    // TODO: send no vote for user1
                }
                builder.show()
            }

            // FOR THE DEMO TOMORROW
            fab_demo.setOnClickListener { v ->
                val builder = AlertDialog.Builder(v.context)
                builder.setTitle(getString(R.string.bounty_payout, priceString, walletId))
                builder.setMessage(
                    getString(
                        R.string.bounty_payout_message,
                        priceString,
                        walletId,
                        voters[0]!!.size,
                        voters[1]!!.size,
                        voters[2]!!.size
                    )
                )
                builder.setPositiveButton("YES") { _, _ ->
                    Toast.makeText(
                        v.context,
                        getString(R.string.bounty_payout_upvoted, priceString, walletId),
                        Toast.LENGTH_SHORT
                    ).show()
                    voters[0]!!.add("user2")
                    voters[2]!!.remove("user2")
                    fab_demo.visibility = View.GONE
                    adapter.notifyChanges()

//                    val walletDir = context?.cacheDir ?: throw Error("CacheDir not found")
//                    val walletService =
//                        WalletService.getInstance(walletDir, (activity as MusicService))
//                    val result = walletService.signUser2()
//                    if (result) {
                    if (voters[2]!!.size == 0) {
                        findNavController().navigateUp()
                    }

                    // TODO: send yes vote for user2
                }

                builder.setNeutralButton("NO") { _, _ ->
                    Toast.makeText(
                        v.context, getString(R.string.bounty_payout_downvoted, priceString, walletId),
                        Toast.LENGTH_SHORT
                    ).show()
                    voters[1]!!.add("user2")
                    voters[2]!!.remove("user2")
                    fab_demo.visibility = View.GONE
                    adapter.notifyChanges()

                    // TODO: send no vote for user2
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
            val simpleDateFormat = SimpleDateFormat("dd-MM-yyyy HH:mm:ss")
            val formatted = simpleDateFormat.format(Date())

            view.findViewById<TextView>(R.id.voter_time).text = formatted.toString()
        }

        return view
    }
}
