package nl.tudelft.trustchain.currencyii.ui.bitcoin

import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import kotlinx.coroutines.*
import nl.tudelft.ipv8.attestation.trustchain.TrustChainBlock
import nl.tudelft.ipv8.util.toHex
import nl.tudelft.trustchain.common.R
import nl.tudelft.trustchain.common.ui.TabsAdapter
import nl.tudelft.trustchain.currencyii.CoinCommunity
import nl.tudelft.trustchain.currencyii.coin.WalletManagerAndroid
import nl.tudelft.trustchain.currencyii.sharedWallet.*
import nl.tudelft.trustchain.currencyii.ui.BaseFragment
import org.bitcoinj.core.*

/**
 * The class for showing the votes fragment. This class is helped by VotesFragmentHelper.kt in Common.
 * It shows the layout in R.layout.fragment_votes in common.
 * A user can here see the (up/down/undecided)votes and send a vote himself
 */
class VotesFragment : BaseFragment(R.layout.fragment_votes) {
    // From common helper class
    private lateinit var tabsAdapter: TabsAdapter
    private lateinit var viewPager: ViewPager2

    private val tabNames = arrayOf("Upvotes", "Downvotes", "Not voted")

    // From the layout class
    private var voters: Array<ArrayList<String>> = arrayOf(ArrayList(), ArrayList(), ArrayList())
    private lateinit var title: TextView
    private lateinit var subTitle: TextView
    private lateinit var requiredVotes: TextView
    private lateinit var voteFab: ExtendedFloatingActionButton
    private lateinit var tabLayout: TabLayout

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_votes, container, false)
    }

    /**
     * When the view is created it puts the correct data on it.
     */
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        title = view.findViewById(R.id.title)
        subTitle = view.findViewById(R.id.sub_title)
        requiredVotes = view.findViewById(R.id.required_votes)
        voteFab = view.findViewById(R.id.fab_user)
        tabLayout = view.findViewById(R.id.tab_layout)
        viewPager = view.findViewById(R.id.viewpager)

        val localArgs = arguments
        if (localArgs is Bundle) {
            val type = localArgs.getString("type")
            val blockId = localArgs.getString("blockId")!!

            // Check which type the block is and set the corresponding data
            if (type == CoinCommunity.SIGNATURE_ASK_BLOCK) {
                signatureAskBlockVotes(blockId)
            } else if (type == CoinCommunity.TRANSFER_FUNDS_ASK_BLOCK) {
                transferFundsAskBlockVotes(blockId)
            }
        }
        // When there are no participants, there is an error and we should return.
        if (voters.isEmpty()) return

        // Set the tabs with the helper class in common
        updateTabsAdapter(0)
    }

    /**
     * Set the tab names with the number of votes in brackets
     */
    private fun updateTabNames() {
        TabLayoutMediator(tabLayout, viewPager) { tab, position ->
            tab.text = tabNames[position] + " (" + voters[position].size + ")"
        }.attach()
    }

    /**
     * Get the selected block via the blockId parsed.
     * @param blockId - the id of the block that is clicked
     * @return TrustChainBlock
     */
    private fun getSelectedBlock(blockId: String): TrustChainBlock? {
        var allBlocks: List<TrustChainBlock> = getCoinCommunity().fetchProposalBlocks()

        val allUsers = getDemoCommunity().getPeers()
        for (peer in allUsers) {
            lifecycleScope.launch {
                trustchain.crawlChain(peer)
            }
            val crawlResult = trustchain
                .getChainByUser(peer.publicKey.keyToBin())
                .filter {
                    it.type == CoinCommunity.SIGNATURE_ASK_BLOCK ||
                        it.type == CoinCommunity.TRANSFER_FUNDS_ASK_BLOCK
                }
            allBlocks = allBlocks + crawlResult
        }
        for (block in allBlocks) {
            if (block.blockId == blockId) return block
        }

        // If we couldn't find the block then move one page up and show a toast with error message
        findNavController().navigateUp()
        Toast.makeText(this.context, "Something went wrong while fetching this block\nYou have ${allBlocks.size} blocks available", Toast.LENGTH_SHORT).show()
        return null
    }

    /**
     * The method for setting the data for join requests
     */
    private fun signatureAskBlockVotes(blockId: String) {
        val walletManager = WalletManagerAndroid.getInstance()
        val myPublicBitcoinKey = walletManager.protocolECKey().publicKeyAsHex
        val myPublicKey = getTrustChainCommunity().myPeer.publicKey.keyToBin()
        val block = getSelectedBlock(blockId) ?: return

        val rawData = SWSignatureAskTransactionData(block.transaction)
        val data = rawData.getData()

        val walletId = data.SW_UNIQUE_ID

        // Get information about the shared wallet
        val sw = getCoinCommunity().discoverSharedWallets()
            .filter { b -> SWJoinBlockTransactionData(b.transaction).getData().SW_UNIQUE_ID == walletId }[0]
        val swData = SWJoinBlockTransactionData(sw.transaction).getData()

        // Get the id of the person that wants to join
        val requestToJoinId = sw.publicKey.toHex()

        // Set the voters so that they are visible in the different kind of tabs
        setVoters(swData.SW_BITCOIN_PKS, data)

        // Check if I have already voted
        userHasAlreadyVoted(myPublicBitcoinKey)

        title.text = data.SW_UNIQUE_PROPOSAL_ID
        subTitle.text = getString(R.string.vote_join_request_message, requestToJoinId, walletId)
        // Check if the proposal can still be met.
        if (getCoinCommunity().canWinJoinRequest(data)) {
            requiredVotes.text = getString(R.string.votes_required, data.SW_SIGNATURES_REQUIRED)
        } else {
            requiredVotes.text = getString(R.string.votes_required_fails, data.SW_SIGNATURES_REQUIRED)
            requiredVotes.setTextColor(Color.RED)
        }
        // Vote functionality
        voteFab.setOnClickListener { v ->
            val builder = AlertDialog.Builder(v.context)
            builder.setTitle(R.string.vote_join_request_title)
            builder.setMessage(getString(R.string.vote_join_request_message, requestToJoinId, walletId))
            builder.setPositiveButton("YES") { _, _ ->
                // Update the voter's list, because I voted yes
                voters[2].remove(myPublicBitcoinKey)
                voters[0].add(myPublicBitcoinKey)

                // Update the GUI
                userHasAlreadyVoted(myPublicBitcoinKey)
                updateTabsAdapter(0)
                Toast.makeText(v.context, getString(R.string.vote_join_request_upvoted, requestToJoinId, walletId), Toast.LENGTH_SHORT).show()

                // Send yes vote
                getCoinCommunity().joinAskBlockReceived(block, myPublicKey, true, requireContext())
                Log.i("Coin", "Voted yes on joining of: ${block.transaction}")
            }

            builder.setNeutralButton("NO") { _, _ ->
                // Update the voter's list, because I voted no
                voters[2].remove(myPublicBitcoinKey)
                voters[1].add(myPublicBitcoinKey)

                // Update the GUI
                userHasAlreadyVoted(myPublicBitcoinKey)
                updateTabsAdapter(1)
                Toast.makeText(v.context, getString(R.string.vote_join_request_downvoted, requestToJoinId, walletId), Toast.LENGTH_SHORT).show()

                // Send no vote
                getCoinCommunity().joinAskBlockReceived(block, myPublicKey, false, requireContext())
                Log.i("Coin", "Voted no on joining of: ${block.transaction}")
            }
            builder.show()
        }

        // Live showing new votes
        GlobalScope.launch {
            while (true) {
                if (areVotesUpdated(data)) {
                    Log.i("Votes", "Votes are updated")
                    withContext(Dispatchers.Main) {
                        setVoters(swData.SW_BITCOIN_PKS, data)
                        updateTabsAdapter(viewPager.currentItem)
                        // Check if I have already voted
                        userHasAlreadyVoted(myPublicBitcoinKey)
                    }
                }
                delay(1000)
            }
        }
    }

    /**
     * Update the tabs adapter
     * Note:
     * tabsAdapter.notifyDataSetChanges() doesn't work somehow, this is hacky, but works.
     */
    private fun updateTabsAdapter(i: Int) {
        tabsAdapter = TabsAdapter(this, voters)
        viewPager.adapter = tabsAdapter
        viewPager.currentItem = i

        updateTabNames()
    }

    /**
     * Check if the votes for the transfer ask block are updated by collecting the new signatures
     * and comparing them to the current ones.
     * @param data - the data about the transfer funds block
     */
    private fun areVotesUpdated(data: SWSignatureAskBlockTD): Boolean {
        // Get the favor and against votes
        val signatures = ArrayList(getCoinCommunity().fetchProposalResponses(data.SW_UNIQUE_ID, data.SW_UNIQUE_PROPOSAL_ID))
        val negativeSignatures = ArrayList(getCoinCommunity().fetchNegativeProposalResponses(data.SW_UNIQUE_ID, data.SW_UNIQUE_PROPOSAL_ID))

        // Recalculate the signatures to the PKs
        val favorPKs = ArrayList(signatures.map { it.SW_BITCOIN_PK })
        val againstPKs = ArrayList(negativeSignatures.map { it.SW_BITCOIN_PK })

        return !(voters[0].size == favorPKs.size && voters[1].size == againstPKs.size)
    }

    /**
     * Check if the votes for the transfer ask block are updated by collecting the new signatures
     * and comparing them to the current ones.
     * @param data - the data about the transfer funds block
     */
    private fun areVotesUpdated(data: SWTransferFundsAskBlockTD): Boolean {
        // Get the favor and against votes
        val signatures = ArrayList(getCoinCommunity().fetchProposalResponses(data.SW_UNIQUE_ID, data.SW_UNIQUE_PROPOSAL_ID))
        val negativeSignatures = ArrayList(getCoinCommunity().fetchNegativeProposalResponses(data.SW_UNIQUE_ID, data.SW_UNIQUE_PROPOSAL_ID))

        // Recalculate the signatures to the PKs
        val favorPKs = ArrayList(signatures.map { it.SW_BITCOIN_PK })
        val againstPKs = ArrayList(negativeSignatures.map { it.SW_BITCOIN_PK })

        return !(voters[0].size == favorPKs.size && voters[1].size == againstPKs.size)
    }

    /**
     * The method for setting the data for transfer funds requests
     */
    private fun transferFundsAskBlockVotes(blockId: String) {
        val walletManager = WalletManagerAndroid.getInstance()
        val myPublicBitcoinKey = walletManager.protocolECKey().publicKeyAsHex
        val myPublicKey = getTrustChainCommunity().myPeer.publicKey.keyToBin()
        val block = getSelectedBlock(blockId) ?: return

        val rawData = SWTransferFundsAskTransactionData(block.transaction)
        val data = rawData.getData()

        val walletId = data.SW_UNIQUE_ID
        val priceString = Coin.valueOf(data.SW_TRANSFER_FUNDS_AMOUNT).toFriendlyString()

        // Get information about the shared wallet
        val sw = getCoinCommunity().discoverSharedWallets()
            .filter { b -> SWJoinBlockTransactionData(b.transaction).getData().SW_UNIQUE_ID == walletId }[0]
        val swData = SWJoinBlockTransactionData(sw.transaction).getData()

        // Set the voters so that they are visible in the different kind of tabs
        setVoters(swData.SW_BITCOIN_PKS, data)

        // Check if I have already voted
        userHasAlreadyVoted(myPublicBitcoinKey)

        title.text = data.SW_UNIQUE_PROPOSAL_ID
        subTitle.text = getString(R.string.bounty_payout, priceString, data.SW_TRANSFER_FUNDS_TARGET_SERIALIZED)
        // Check if the proposal can still be met.
        if (getCoinCommunity().canWinTransferRequest(data)) {
            requiredVotes.text = getString(R.string.votes_required, data.SW_SIGNATURES_REQUIRED)
        } else {
            requiredVotes.text = getString(R.string.votes_required_fails, data.SW_SIGNATURES_REQUIRED)
            requiredVotes.setTextColor(Color.RED)
        }
        // Vote functionality
        voteFab.setOnClickListener { v ->
            val builder = AlertDialog.Builder(v.context)
            builder.setTitle(getString(R.string.bounty_payout, priceString, walletId))
            builder.setMessage(
                getString(
                    R.string.bounty_payout_message,
                    priceString,
                    data.SW_TRANSFER_FUNDS_TARGET_SERIALIZED
                )
            )
            builder.setPositiveButton("YES") { _, _ ->
                // Update the voter's list, because I voted yes
                voters[2].remove(myPublicBitcoinKey)
                voters[0].add(myPublicBitcoinKey)

                // Update the GUI
                userHasAlreadyVoted(myPublicBitcoinKey)
                updateTabsAdapter(0)
                Toast.makeText(v.context, getString(R.string.bounty_payout_upvoted, priceString, walletId), Toast.LENGTH_SHORT).show()

                // Send yes vote
                getCoinCommunity().transferFundsBlockReceived(block, myPublicKey, true, requireContext())
                Log.i("Coin", "Voted yes on transferring funds of: ${block.transaction}")
            }

            builder.setNeutralButton("NO") { _, _ ->
                // Update the voter's list, because I voted no
                voters[2].remove(myPublicBitcoinKey)
                voters[1].add(myPublicBitcoinKey)

                // Update the GUI
                userHasAlreadyVoted(myPublicBitcoinKey)
                updateTabsAdapter(1)
                Toast.makeText(v.context, getString(R.string.bounty_payout_downvoted, priceString, walletId), Toast.LENGTH_SHORT).show()

                // Send no vote
                getCoinCommunity().transferFundsBlockReceived(block, myPublicKey, false, requireContext())
                Log.i("Coin", "Voted yes on transferring funds of: ${block.transaction}")
            }
            builder.show()
        }

        // Live showing new votes
        GlobalScope.launch {
            while (true) {
                if (areVotesUpdated(data)) {
                    withContext(Dispatchers.Main) {
                        setVoters(swData.SW_BITCOIN_PKS, data)
                        updateTabsAdapter(viewPager.currentItem)
                        // Check if I have already voted
                        userHasAlreadyVoted(myPublicBitcoinKey)
                    }
                }
                delay(1000)
            }
        }
    }

    /**
     * When the user has already voted, or made a vote
     * It hides the vote button and maybe something more in the future.
     */
    private fun userHasAlreadyVoted(myPublicBitcoinKey: String) {
        if (!voters[2].contains(myPublicBitcoinKey)) {
            voteFab.visibility = View.GONE
        }
    }

    /**
     * Set the voters, ordered by if they voted in favor, against or not.
     * @param participants - All the participants of the DAO.
     * @param data - The data about the proposal block
     */
    private fun setVoters(
        participants: ArrayList<String>,
        data: SWSignatureAskBlockTD
    ) {
        // Get the favor and against votes
        val signatures = ArrayList(getCoinCommunity().fetchProposalResponses(data.SW_UNIQUE_ID, data.SW_UNIQUE_PROPOSAL_ID))
        val negativeSignatures = ArrayList(getCoinCommunity().fetchNegativeProposalResponses(data.SW_UNIQUE_ID, data.SW_UNIQUE_PROPOSAL_ID))

        // Recalculate the signatures to the PKs
        val favorPKs = ArrayList(signatures.map { it.SW_BITCOIN_PK })
        val againstPKs = ArrayList(negativeSignatures.map { it.SW_BITCOIN_PK })

        updateVotersList(favorPKs, againstPKs, participants)
    }

    /**
     * Set the voters, ordered by if they voted in favor, against or not.
     * @param participants - All the participants of the DAO.
     * @param data - The data about the proposal block
     */
    private fun setVoters(participants: ArrayList<String>, data: SWTransferFundsAskBlockTD) {
        // Get the favor and against votes
        val signatures = ArrayList(getCoinCommunity().fetchProposalResponses(data.SW_UNIQUE_ID, data.SW_UNIQUE_PROPOSAL_ID))
        val negativeSignatures = ArrayList(getCoinCommunity().fetchNegativeProposalResponses(data.SW_UNIQUE_ID, data.SW_UNIQUE_PROPOSAL_ID))

        // Recalculate the signatures to the PKs
        val favorPKs = ArrayList(signatures.map { it.SW_BITCOIN_PK })
        val againstPKs = ArrayList(negativeSignatures.map { it.SW_BITCOIN_PK })

        updateVotersList(favorPKs, againstPKs, participants)
    }

    /**
     * Set the voters in the correct position of the voters list
     * @param favorPKs - The list of Bitcoin PKs that have voted in favor of the proposal
     * @param againstPKs - The list of Bitcoin PKs that have voted against the proposal
     * @param participants - The list of Bitcoin PKs that are in total in the DAO
     */
    private fun updateVotersList(
        favorPKs: ArrayList<String>,
        againstPKs: ArrayList<String>,
        participants: ArrayList<String>
    ) {
        voters[0] = favorPKs
        voters[1] = againstPKs
        voters[2] = participants

        // If a user has already voted remove their entry from the participants
        for (favorPK in favorPKs) {
            voters[2].remove(favorPK)
        }
        for (againstPk in againstPKs) {
            voters[2].remove(againstPk)
        }
    }
}
