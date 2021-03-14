package nl.tudelft.trustchain.currencyii.ui.bitcoin

import android.graphics.Color
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
import nl.tudelft.ipv8.attestation.trustchain.TrustChainBlock
import nl.tudelft.ipv8.util.hexToBytes
import nl.tudelft.ipv8.util.toHex
import nl.tudelft.trustchain.common.R
import nl.tudelft.trustchain.common.ui.TabsAdapter
import nl.tudelft.trustchain.currencyii.CoinCommunity
import nl.tudelft.trustchain.currencyii.sharedWallet.SWJoinBlockTransactionData
import nl.tudelft.trustchain.currencyii.sharedWallet.SWSignatureAskTransactionData
import nl.tudelft.trustchain.currencyii.sharedWallet.SWTransferFundsAskTransactionData
import nl.tudelft.trustchain.currencyii.ui.BaseFragment
import org.bitcoinj.core.ECKey

/**
 * The class for showing the votes fragment. This class is helped by VotesFragmentHelper.kt in Common.
 * It shows the layout in R.layout.fragment_votes in common.
 * A user can here see the (up/down/undecided)votes and send a vote himself
 */
class VotesFragment : BaseFragment(R.layout.fragment_votes) {
    // From common helper class
    private lateinit var tabsAdapter: TabsAdapter
    private lateinit var viewPager: ViewPager2

    private val TAB_NAMES = arrayOf("Upvotes", "Downvotes", "Not voted")

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
        tabsAdapter = TabsAdapter(this, voters)
        viewPager.adapter = tabsAdapter

        updateTabNames()
    }

    /**
     * Set the tab names with the number of votes in brackets
     */
    private fun updateTabNames() {
        TabLayoutMediator(tabLayout, viewPager) { tab, position ->
            tab.text = TAB_NAMES[position] + " (" + voters[position].size + ")"
        }.attach()
    }

    /**
     * Get the selected block via the blockId parsed.
     * @param blockId - the id of the block that is clicked
     * @return TrustChainBlock
     */
    private fun getSelectedBlock(blockId: String): TrustChainBlock? {
        val allBlocks = getCoinCommunity().fetchProposalBlocks()
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
        val myPublicKey = getTrustChainCommunity().myPeer.publicKey.keyToBin()
        val block = getSelectedBlock(blockId) ?: return

        val rawData = SWSignatureAskTransactionData(block.transaction)
        val data = rawData.getData()

        val walletId = data.SW_UNIQUE_ID

        // TODO: Crashes when user has no wallet, but that isn't possible otherwise he shouldn't see the proposal at the first place.
        // Get information about the shared wallet
        val sw = getCoinCommunity().discoverSharedWallets()
            .filter { b -> SWJoinBlockTransactionData(b.transaction).getData().SW_UNIQUE_ID == walletId }[0]
        val swData = SWJoinBlockTransactionData(sw.transaction).getData()

        // Get the id of the person that wants to join
        val requestToJoinId = sw.publicKey.toHex()

        // Get my signature
        val mySignature = getCoinCommunity().getMySignatureJoinRequest(data)
        val mySignatureSerialized = mySignature.encodeToDER().toHex()

        // TODO get the PK of the users that already voted, the signatures aren't the same, but at least they represent the number of upvotes
        // Get the favor and against votes
        val signatures = ArrayList(getCoinCommunity().fetchProposalSignatures(data.SW_UNIQUE_ID, data.SW_UNIQUE_PROPOSAL_ID))
        val negativeSignatures = ArrayList(getCoinCommunity().fetchNegativeProposalSignatures(data.SW_UNIQUE_ID, data.SW_UNIQUE_PROPOSAL_ID))

        // TODO: get the PKs
        // Recalculate the signatures to the PKs
        val favorPKs = ArrayList(signatures.map { getPK(it, swData.SW_BITCOIN_PKS, data.SW_UNIQUE_ID, data.SW_UNIQUE_PROPOSAL_ID) })
        val againstPKs = ArrayList(negativeSignatures.map { getPK(it, swData.SW_BITCOIN_PKS, data.SW_UNIQUE_ID, data.SW_UNIQUE_PROPOSAL_ID) })

        // Set the voters so that they are visible in the different kind of tabs
        setVoters(swData.SW_TRUSTCHAIN_PKS, favorPKs, againstPKs)

        // Check if I have already voted
        val userHasVoted = signatures.contains(mySignatureSerialized) || negativeSignatures.contains(
            mySignatureSerialized
        )

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
                voters[2].remove(myPublicKey.toHex())
                voters[0].add(myPublicKey.toHex())

                // Update the GUI
                userHasAlreadyVoted()
                tabsAdapter = TabsAdapter(this, voters)
                viewPager.adapter = tabsAdapter
                viewPager.currentItem = 0
                Toast.makeText(v.context, getString(R.string.vote_join_request_upvoted, requestToJoinId, walletId), Toast.LENGTH_SHORT).show()
                updateTabNames()

                // Send yes vote
                getCoinCommunity().joinAskBlockReceived(block, myPublicKey, true)

                // TODO: Check if the proposal is met
                if (voters[2].isEmpty()) {
                    findNavController().navigateUp()
                }
            }

            builder.setNeutralButton("NO") { _, _ ->
                // Update the voter's list, because I voted no
                voters[2].remove(myPublicKey.toHex())
                voters[1].add(myPublicKey.toHex())

                // Update the GUI
                userHasAlreadyVoted()
                tabsAdapter = TabsAdapter(this, voters)
                viewPager.adapter = tabsAdapter
                viewPager.currentItem = 1
                Toast.makeText(v.context, getString(R.string.vote_join_request_downvoted, requestToJoinId, walletId), Toast.LENGTH_SHORT).show()
                updateTabNames()

                // Send no vote
                getCoinCommunity().joinAskBlockReceived(block, myPublicKey, false)
            }
            builder.show()
        }

        if (userHasVoted) {
            userHasAlreadyVoted()
        }
    }

    /**
     * The method for setting the data for transfer funds requests
     */
    private fun transferFundsAskBlockVotes(blockId: String) {
        val myPublicKey = getTrustChainCommunity().myPeer.publicKey.keyToBin()
        val block = getSelectedBlock(blockId) ?: return

        val rawData = SWTransferFundsAskTransactionData(block.transaction)
        val data = rawData.getData()

        val walletId = data.SW_UNIQUE_ID
        val priceString = data.SW_TRANSFER_FUNDS_AMOUNT.toString() + " Satoshi"

        // TODO: Crashes when user has no wallet, but that isn't possible otherwise he shouldn't see the proposal at the first place.
        // Get information about the shared wallet
        val sw = getCoinCommunity().discoverSharedWallets()
            .filter { b -> SWJoinBlockTransactionData(b.transaction).getData().SW_UNIQUE_ID == walletId }[0]
        val swData = SWJoinBlockTransactionData(sw.transaction).getData()

        // Get my signature
        val mySignature = getCoinCommunity().getMySignatureTransaction(data)
        val mySignatureSerialized = mySignature.encodeToDER().toHex()


        // TODO get the PK of the users that already voted, the signatures aren't the same, but at least they represent the number of upvotes
        // Get the favor and against votes
        val signatures = ArrayList(getCoinCommunity().fetchProposalSignatures(data.SW_UNIQUE_ID, data.SW_UNIQUE_PROPOSAL_ID))
        val negativeSignatures = ArrayList(getCoinCommunity().fetchNegativeProposalSignatures(data.SW_UNIQUE_ID, data.SW_UNIQUE_PROPOSAL_ID))

        // TODO: get the PKs
        // Recalculate the signatures to the PKs
        val favorPKs = ArrayList(signatures.map { getPK(it, swData.SW_BITCOIN_PKS, data.SW_UNIQUE_ID, data.SW_UNIQUE_PROPOSAL_ID) })
        val againstPKs = ArrayList(negativeSignatures.map { getPK(it, swData.SW_BITCOIN_PKS, data.SW_UNIQUE_ID, data.SW_UNIQUE_PROPOSAL_ID) })

        // Set the voters so that they are visible in the different kind of tabs
        setVoters(swData.SW_TRUSTCHAIN_PKS, favorPKs, againstPKs)

        // Check if I have already voted
        val userHasVoted = signatures.contains(mySignatureSerialized) || negativeSignatures.contains(
            mySignatureSerialized
        )

        title.text = data.SW_UNIQUE_PROPOSAL_ID
        subTitle.text = getString(R.string.bounty_payout, priceString, walletId)
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
                    walletId,
                    voters[0].size,
                    voters[1].size,
                    voters[2].size
                )
            )
            builder.setPositiveButton("YES") { _, _ ->
                // Update the voter's list, because I voted yes
                voters[2].remove(myPublicKey.toHex())
                voters[0].add(myPublicKey.toHex())

                // Update the GUI
                userHasAlreadyVoted()
                tabsAdapter = TabsAdapter(this, voters)
                viewPager.adapter = tabsAdapter
                viewPager.currentItem = 0
                Toast.makeText(v.context, getString(R.string.bounty_payout_upvoted, priceString, walletId), Toast.LENGTH_SHORT).show()
                updateTabNames()

                // Send yes vote
                getCoinCommunity().transferFundsBlockReceived(block, myPublicKey, true)

                // TODO: Check if the proposal is met
                if (voters[2].isEmpty()) {
                    findNavController().navigateUp()
                }
            }

            builder.setNeutralButton("NO") { _, _ ->
                // Update the voter's list, because I voted no
                voters[2].remove(myPublicKey.toHex())
                voters[1].add(myPublicKey.toHex())

                // Update the GUI
                userHasAlreadyVoted()
                tabsAdapter = TabsAdapter(this, voters)
                viewPager.adapter = tabsAdapter
                viewPager.currentItem = 1
                Toast.makeText(v.context, getString(R.string.bounty_payout_downvoted, priceString, walletId), Toast.LENGTH_SHORT).show()
                updateTabNames()

                // Send no vote
                getCoinCommunity().transferFundsBlockReceived(block, myPublicKey, false)
            }
            builder.show()
        }

        if (userHasVoted) {
            userHasAlreadyVoted()
        }
    }

    /**
     * When the user has already voted, or made a vote
     * It hides the vote button and maybe something more in the future.
     */
    private fun userHasAlreadyVoted() {
        voteFab.visibility = View.GONE
    }

    /**
     * TODO:
     * Get the primary corresponding to the signature.
     * @param
     * @return String - Public Key
     */
    private fun getPK(
        signature: String,
        bitcoin_pks: ArrayList<String>,
        walletId: String,
        proposalId: String
    ): String {
        val signatureKey = ECKey.ECDSASignature.decodeFromDER(signature.hexToBytes())

        for (pk in bitcoin_pks) {

//            val block = getTrustChainCommunity().database.getBlocksWithType(CoinCommunity.SIGNATURE_AGREEMENT_NEGATIVE_BLOCK)
//                .filter {
//                    val blockData = SWResponseNegativeSignatureTransactionData(it.transaction)
//                    blockData.matchesProposal(walletId, proposalId) && blockData.getData().SW_SIGNATURE_SERIALIZED == signature
//                }[0]

            val block = getTrustChainCommunity().database.getBlocksWithType(CoinCommunity.SIGNATURE_ASK_BLOCK).filter {
                val blockData = SWSignatureAskTransactionData(it.transaction).getData()
                blockData.SW_UNIQUE_ID == walletId && blockData.SW_UNIQUE_PROPOSAL_ID == proposalId
            }[0]

            val temp = SWSignatureAskTransactionData(block.transaction).getData().SW_TRANSACTION_SERIALIZED

            val result = ECKey.verify(temp.hexToBytes(), signatureKey, pk.hexToBytes())


            println(result)
            println(bitcoin_pks)
//            println(swTransactionSerialized)
        }

        return "Unknown signature found"
    }

    /**
     * Set the voters, ordered by if they voted in favor, against or not.
     * @param participants - All the participants of the DAO.
     * @param favorPKs - All the primary keys of people that already voted in favor.
     * @param againstPKs - All the primary keys of people that already voted against.
     */
    private fun setVoters(participants: ArrayList<String>, favorPKs: ArrayList<String>, againstPKs: ArrayList<String>) {
        voters[0] = favorPKs
        voters[1] = againstPKs
        voters[2] = participants

        // If a user has already voted remove their entry from the participants
        voters[2].removeAll(againstPKs)
        voters[2].removeAll(favorPKs)
    }
}
