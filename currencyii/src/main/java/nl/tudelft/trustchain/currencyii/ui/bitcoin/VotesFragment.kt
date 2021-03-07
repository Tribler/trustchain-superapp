package nl.tudelft.trustchain.currencyii.ui.bitcoin

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.navigation.fragment.findNavController
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import nl.tudelft.trustchain.common.R
import nl.tudelft.trustchain.common.ui.Adapter
import nl.tudelft.trustchain.common.ui.BaseFragment

class VotesFragment : BaseFragment(R.layout.fragment_votes) {
    private lateinit var adapter: Adapter
    private lateinit var viewPager: ViewPager2

    private val TAB_NAMES = arrayOf("Upvotes", "Downvotes", "Undecided votes")

    private lateinit var voters: HashMap<Int, ArrayList<String>>

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_votes, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {

        val localArgs = arguments
        if (localArgs is Bundle) {
            val walletId = localArgs.getString("title", "Title not found")
            val priceString = localArgs.getLong("amount").toString() + "BTC"
            val userID = localArgs.getString("userID")!!

            @Suppress("UNCHECKED_CAST")
            voters = localArgs.get("voters") as HashMap<Int, ArrayList<String>>
//            val block: TrustChainBlock = localArgs.get("Block") as TrustChainBlock
            val signatures = localArgs.getStringArrayList("signatures")
            voters[0] = signatures!!
            voters[2]!!.removeAll(signatures)
            val userHasVoted = voters[2]!!.contains(userID)

            view.findViewById<TextView>(R.id.title).text = walletId
            view.findViewById<TextView>(R.id.price).text = getString(R.string.bounty_payout, priceString, walletId)
            view.findViewById<ExtendedFloatingActionButton>(R.id.fab_user).setOnClickListener { v ->
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
                    voters[2]!!.remove(userID)
                    voters[0]!!.add(userID)
//                    voters = SWTransferFundsAskTransactionData(block.transaction).userVotes(userID, 0)

                    userHasAlreadyVoted(view)
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
                        v.context,
                        getString(R.string.bounty_payout_downvoted, priceString, walletId),
                        Toast.LENGTH_SHORT
                    ).show()
                    voters[2]!!.remove(userID)
                    voters[1]!!.add(userID)
//                    voters = SWTransferFundsAskTransactionData(block.transaction).userVotes(userID, 1)

                    userHasAlreadyVoted(view)
                    adapter.notifyChanges()

                    // TODO: send no vote for user1
                }
                builder.show()
            }

            if (userHasVoted) {
                userHasAlreadyVoted(view)
            }
        }

        adapter = Adapter(this, voters)
        viewPager = view.findViewById(R.id.viewpager)
        viewPager.adapter = adapter

        val tabLayout = view.findViewById<TabLayout>(R.id.tab_layout)
        TabLayoutMediator(tabLayout, viewPager) { tab, position ->
            tab.text = TAB_NAMES[position]
        }.attach()
    }

    private fun userHasAlreadyVoted(view: View) {
        view.findViewById<ExtendedFloatingActionButton>(R.id.fab_user).visibility = View.GONE
    }
}
