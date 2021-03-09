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
import nl.tudelft.ipv8.util.toHex
import nl.tudelft.trustchain.common.R
import nl.tudelft.trustchain.common.ui.TabsAdapter
import nl.tudelft.trustchain.currencyii.CoinCommunity
import nl.tudelft.trustchain.currencyii.coin.WalletManager
import nl.tudelft.trustchain.currencyii.sharedWallet.SWBlockTransactionData
import nl.tudelft.trustchain.currencyii.sharedWallet.SWJoinBlockTransactionData
import nl.tudelft.trustchain.currencyii.sharedWallet.SWSignatureAskTransactionData
import nl.tudelft.trustchain.currencyii.sharedWallet.SWTransferFundsAskTransactionData
import nl.tudelft.trustchain.currencyii.ui.BaseFragment
import org.json.JSONObject

class VotesFragment : BaseFragment(R.layout.fragment_votes) {
    private lateinit var tabsAdapter: TabsAdapter
    private lateinit var viewPager: ViewPager2

    private val TAB_NAMES = arrayOf("Upvotes", "Downvotes", "Undecided votes")

    private lateinit var voters: HashMap<Int, ArrayList<String>>
    private lateinit var title: TextView
    private lateinit var price: TextView
    private lateinit var demoVoteFab: ExtendedFloatingActionButton
    private lateinit var voteFab: ExtendedFloatingActionButton

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_votes, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        title = view.findViewById(R.id.title)
        price = view.findViewById(R.id.price)
        demoVoteFab = view.findViewById(R.id.fab_demo)
        voteFab = view.findViewById(R.id.fab_user)

        demoVoteFab.visibility = View.GONE

        val localArgs = arguments
        if (localArgs is Bundle) {
            val position = localArgs.getInt("position")
            val type = localArgs.getString("type")

            if (type == CoinCommunity.SIGNATURE_ASK_BLOCK) {
                signatureAskBlockVotes(position)
            } else if (type == CoinCommunity.TRANSFER_FUNDS_ASK_BLOCK) {
                transferFundsAskBlockVotes(position)
            }
        }

        viewPager = view.findViewById(R.id.viewpager)
        tabsAdapter = TabsAdapter(this, voters)
        viewPager.adapter = tabsAdapter

        val tabLayout = view.findViewById<TabLayout>(R.id.tab_layout)
        TabLayoutMediator(tabLayout, viewPager) { tab, position ->
            tab.text = TAB_NAMES[position]
        }.attach()
    }

    private fun signatureAskBlockVotes(position: Int) {
        val myPublicKey = getTrustChainCommunity().myPeer.publicKey.keyToBin().toHex()
        val block = getCoinCommunity().fetchProposalBlocks()[position]

        val rawData = SWSignatureAskTransactionData(block.transaction)
        val data = rawData.getData()

        val walletId = data.SW_UNIQUE_ID

        val sw = getCoinCommunity().discoverSharedWallets().filter { b -> SWJoinBlockTransactionData(b.transaction).getData().SW_UNIQUE_ID == walletId }[0]
        val swData = SWJoinBlockTransactionData(sw.transaction).getData()

        val requestToJoinId = sw.publicKey.toHex()

        // TODO get the actual votes, instead of only the participants
        voters = hashMapOf(0 to arrayListOf(), 1 to arrayListOf(), 2 to swData.SW_TRUSTCHAIN_PKS)

        // TODO get the signatures that already voted
//        val signatures =
//            ArrayList(
//                getCoinCommunity().fetchProposalSignatures(
//                    data.SW_UNIQUE_ID,
//                    data.SW_UNIQUE_PROPOSAL_ID
//                )
//            )
//        voters[0] = signatures
//        voters[2]!!.removeAll(signatures)

        val userHasVoted = !voters[2]!!.contains(myPublicKey)

        title.text = data.SW_UNIQUE_PROPOSAL_ID
        price.text = getString(R.string.vote_join_request_message, requestToJoinId, walletId)
        voteFab.setOnClickListener { v ->
            val builder = AlertDialog.Builder(v.context)
            builder.setTitle(R.string.vote_join_request_title)
            builder.setMessage(getString(R.string.vote_join_request_message, requestToJoinId, walletId))
            builder.setPositiveButton("YES") { _, _ ->
                // Update the voter's list, because I voted yes
                voters = SWSignatureAskTransactionData(block.transaction).userVotes(myPublicKey, 0)

                // Update the GUI
                userHasAlreadyVoted()
                tabsAdapter = TabsAdapter(this, voters)
                viewPager.adapter = tabsAdapter
                viewPager.currentItem = 0
                Toast.makeText(v.context, getString(R.string.vote_join_request_upvoted, requestToJoinId, walletId), Toast.LENGTH_SHORT).show()

                // TODO: send yes vote for user
                if (voters[2]!!.size == 0) {
                    findNavController().navigateUp()
                }
            }

            builder.setNeutralButton("NO") { _, _ ->
                // Update the voter's list, because I voted no
                voters = SWSignatureAskTransactionData(block.transaction).userVotes(myPublicKey, 1)

                // Update the GUI
                userHasAlreadyVoted()
                tabsAdapter = TabsAdapter(this, voters)
                viewPager.adapter = tabsAdapter
                viewPager.currentItem = 1
                Toast.makeText(v.context, getString(R.string.vote_join_request_downvoted, requestToJoinId, walletId), Toast.LENGTH_SHORT).show()

                // TODO: send no vote for user
            }
            builder.show()
        }

        if (userHasVoted) {
            userHasAlreadyVoted()
        }
    }

    private fun transferFundsAskBlockVotes(position: Int) {
        val myPublicKey = getTrustChainCommunity().myPeer.publicKey.keyToBin().toHex()
        val block = getCoinCommunity().fetchProposalBlocks()[position]

        val rawData = SWTransferFundsAskTransactionData(block.transaction)
        val data = rawData.getData()

        val walletId = data.SW_UNIQUE_ID
        val priceString = data.SW_TRANSFER_FUNDS_AMOUNT.toString() + " Satoshi"


        val sw = getCoinCommunity().discoverSharedWallets().filter { b -> SWJoinBlockTransactionData(b.transaction).getData().SW_UNIQUE_ID == walletId }[0]
        val swData = SWJoinBlockTransactionData(sw.transaction).getData()

        // TODO get the actual votes, instead of only the participants
        voters = hashMapOf(0 to arrayListOf(), 1 to arrayListOf(), 2 to swData.SW_TRUSTCHAIN_PKS)

        // TODO get the signatures that already voted
//        val signatures =
//            ArrayList(
//                getCoinCommunity().fetchProposalSignatures(
//                    data.SW_UNIQUE_ID,
//                    data.SW_UNIQUE_PROPOSAL_ID
//                )
//            )
//        voters[0] = signatures
//        voters[2]!!.removeAll(signatures)

        val userHasVoted = !voters[2]!!.contains(myPublicKey)

        title.text = data.SW_UNIQUE_PROPOSAL_ID
        price.text = getString(R.string.bounty_payout, priceString, walletId)
        voteFab.setOnClickListener { v ->
            val builder = AlertDialog.Builder(v.context)
            builder.setTitle(getString(R.string.bounty_payout, priceString, walletId))
            builder.setMessage(getString(
                    R.string.bounty_payout_message,
                    priceString,
                    walletId,
                    voters[0]!!.size,
                    voters[1]!!.size,
                    voters[2]!!.size
                ))
            builder.setPositiveButton("YES") { _, _ ->
                // Update the voter's list, because I voted yes
                voters = SWTransferFundsAskTransactionData(block.transaction).userVotes(myPublicKey, 0)

                userHasAlreadyVoted()
                tabsAdapter = TabsAdapter(this, voters)
                viewPager.adapter = tabsAdapter
                viewPager.currentItem = 0
                Toast.makeText(v.context, getString(R.string.bounty_payout_upvoted, priceString, walletId), Toast.LENGTH_SHORT).show()

                // TODO: send yes vote for user
                if (voters[2]!!.size == 0) {
                    findNavController().navigateUp()
                }
            }

            builder.setNeutralButton("NO") { _, _ ->
                // Update the voter's list, because I voted no
                voters = SWTransferFundsAskTransactionData(block.transaction).userVotes(myPublicKey, 1)

                // Update the GUI
                userHasAlreadyVoted()
                tabsAdapter = TabsAdapter(this, voters)
                viewPager.adapter = tabsAdapter
                viewPager.currentItem = 1
                Toast.makeText(v.context, getString(R.string.bounty_payout_downvoted, priceString, walletId), Toast.LENGTH_SHORT).show()

                // TODO: send no vote for user
            }
            builder.show()
        }

        if (userHasVoted) {
            userHasAlreadyVoted()
        }
    }

    private fun userHasAlreadyVoted() {
        voteFab.visibility = View.GONE
    }
}
