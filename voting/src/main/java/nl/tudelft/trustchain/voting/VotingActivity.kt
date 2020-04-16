package nl.tudelft.trustchain.voting

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.os.Bundle
import android.text.Html
import android.text.InputType
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import kotlinx.android.synthetic.main.activity_main_voting.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import nl.tudelft.ipv8.android.IPv8Android
import nl.tudelft.ipv8.attestation.trustchain.TrustChainBlock
import nl.tudelft.ipv8.attestation.trustchain.TrustChainCommunity
import nl.tudelft.ipv8.keyvault.PublicKey
import nl.tudelft.ipv8.keyvault.defaultCryptoProvider
import nl.tudelft.trustchain.common.util.TrustChainHelper
import nl.tudelft.trustchain.common.util.VotingHelper
import org.json.JSONException
import org.json.JSONObject

class VotingActivity : AppCompatActivity() {

    private lateinit var vh: VotingHelper
    private lateinit var community: TrustChainCommunity
    private lateinit var adapter: blockListAdapter
    private lateinit var tch: TrustChainHelper

    private var voteProposals: MutableList<TrustChainBlock> = mutableListOf()
    private var displayAllVotes: Boolean = true

    /**
     * Setup method, binds functionality
     */
    @SuppressLint("SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main_voting)
        title = "TrustChain Voter"

        initiateButton.setOnClickListener {
            showNewVoteDialog()
        }

        uncastedToggle.setOnCheckedChangeListener { _, isChecked ->
            displayAllVotes = if (isChecked) {
                proposalOverViewTitle.text = "New Proposals"
                printShortToast("Displaying proposals to cast on")
                false
            } else {
                proposalOverViewTitle.text = "All Proposals"
                printShortToast("Displaying all proposals")
                true
            }
        }

        // Initiate community and helpers
        val ipv8 = IPv8Android.getInstance()
        community = ipv8.getOverlay()!!
        vh = VotingHelper(community)
        tch = TrustChainHelper(community)

        // Styling
        blockList.addItemDecoration(DividerItemDecoration(this, LinearLayoutManager.VERTICAL))
        blockList.layoutManager = LinearLayoutManager(this)

        adapter = blockListAdapter(voteProposals)

        adapter.onItemClick = {
            showNewCastVoteDialog(it)
        }

        blockList.adapter = adapter

        periodicUpdate()
    }

    /**
     * Display a short message on the screen
     */
    private fun printShortToast(s: String) {
        Toast.makeText(this, s, Toast.LENGTH_SHORT).show()
    }

    /**
     * Dialog for creating a new proposal
     */
    private fun showNewVoteDialog() {
        val builder: AlertDialog.Builder = AlertDialog.Builder(this)
        builder.setTitle("Create proposal")

        val input = EditText(this)
        input.inputType = InputType.TYPE_CLASS_TEXT
        input.hint = "p != np"
        builder.setView(input)

        // PositiveButton is always the rightmost button
        builder.setPositiveButton("Create") { _, _ ->
            val proposal = input.text.toString()

            // Create list of your peers and include yourself
            val peers: MutableList<PublicKey> = ArrayList()
            peers.addAll(community.getPeers().map { it.publicKey })
            peers.add(community.myPeer.publicKey)

            // Start voting procedure
            vh.startVote(proposal, peers)
            printShortToast("Proposal has been created")
        }

        // NeutralButton is always the leftmost button
        builder.setNeutralButton("Cancel") { dialog, _ ->
            dialog.cancel()
        }

        builder.show()
    }

    /**
     * Show dialog from which the user can propose a vote
     */
    private fun showNewCastVoteDialog(block: TrustChainBlock) {
        val builder: AlertDialog.Builder = AlertDialog.Builder(this)

        // Parse the 'message' field as JSON.
        var voteSubject = ""
        try {
            val voteJSON = JSONObject(block.transaction["message"].toString())
            voteSubject = voteJSON.get("VOTE_SUBJECT").toString()
        } catch (e: JSONException) {
            "Block was a voting block but did not contain " +
                "proper JSON in its message field: ${block.transaction["message"]}."
        }

        val previouslyCastedVotes = vh.castedByPeer(block, community.myPeer.publicKey)
        val hasCasted = when {
            previouslyCastedVotes.first == 1 -> {
                "Yes"
            }
            previouslyCastedVotes.second == 1 -> {
                "No"
            }
            else -> {
                null
            }
        }

        val castedString = if (hasCasted != null) {
            "<br><br>" +
                "<small><b>You have voted</b>: <i>" +
                hasCasted +
                "</i></small>"
        } else {
            ""
        }

        // Get tally values
        val tally = getTally(voteSubject, block)

        // Show vote subject, proposer and current tally
        builder.setMessage(
            Html.fromHtml(
                "<big>\"" + voteSubject + "\"</big>" +
                    "<br><br>" +
                    "<small><b>Proposed by</b>:" +
                    "<br>" +
                    "<i>" + defaultCryptoProvider.keyFromPublicBin(block.publicKey) + "</i></small>" +
                    "<br><br>" +
                    "<small><b>Date</b>: " +
                    "<i>" + block.timestamp + "</i></small>" +
                    castedString +
                    "<br><br>" +
                    "<small><b>Current tally</b>:" +
                    "<br>" +
                    "Yes votes: " + tally.first +
                    " | No votes: " + tally.second + "</small></i>",
                Html.FROM_HTML_MODE_LEGACY))

        // Display vote options is not previously casted a vote
        if (hasCasted == null) {
            builder.setTitle("Cast vote on proposal:")

            // PositiveButton is always the rightmost button
            builder.setPositiveButton("YES") { _, _ ->
                vh.respondToVote(true, block)
                printShortToast("You voted: YES")
            }

            // NegativeButton is always second-from-right button
            builder.setNegativeButton("NO") { _, _ ->
                vh.respondToVote(false, block)
                printShortToast("You voted: NO")
            }

            // NeutralButton is always the leftmost button
            builder.setNeutralButton("CANCEL") { dialog, _ ->
                printShortToast("No vote was cast")
                dialog.cancel()
            }
        } else {
            builder.setTitle("Inspect proposal:")
            // NeutralButton is always the leftmost button
            builder.setNeutralButton("Exit") { dialog, _ ->
                dialog.cancel()
            }
        }

        builder.setCancelable(true)

        builder.show()
    }

    /**
     * Count votes and return tally
     */
    fun getTally(voteSubject: String, block: TrustChainBlock): Pair<Int, Int> {
        val peers: MutableList<PublicKey> = ArrayList()
        peers.addAll(community.getPeers().map { it.publicKey })
        peers.add(community.myPeer.publicKey)
        return vh.countVotes(peers, voteSubject, block.publicKey)
    }

    /**
     * Periodically update vote proposal set
     */
    private fun periodicUpdate() {
        lifecycleScope.launchWhenStarted {
            while (isActive) {
                val currentProposals = tch.getBlocksByType("voting_block").filter {
                    !JSONObject(it.transaction["message"].toString()).has("VOTE_REPLY") && displayBlock(
                        it
                    )
                }.asReversed()

                // Update vote proposal set
                if (voteProposals != currentProposals) {
                    voteProposals.clear()
                    voteProposals.addAll(currentProposals)
                    adapter.notifyDataSetChanged()
                }

                delay(1000)
            }
        }
    }

    /**
     * Check if proposal block should be displayed
     */
    private fun displayBlock(block: TrustChainBlock): Boolean {
        if (displayAllVotes) return true
        val votePair = vh.castedByPeer(block, community.myPeer.publicKey)
        return votePair == Pair(0, 0)
    }
}
