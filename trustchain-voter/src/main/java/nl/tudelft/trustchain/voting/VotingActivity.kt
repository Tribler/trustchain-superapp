package nl.tudelft.trustchain.voting

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.os.Bundle
import android.text.format.DateFormat
import android.view.LayoutInflater
import android.widget.EditText
import android.widget.Switch
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.text.HtmlCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import kotlinx.android.synthetic.main.activity_main_voting.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import nl.tudelft.ipv8.android.IPv8Android
import nl.tudelft.ipv8.attestation.trustchain.TrustChainBlock
import nl.tudelft.ipv8.keyvault.PublicKey
import nl.tudelft.ipv8.keyvault.defaultCryptoProvider
import nl.tudelft.trustchain.common.util.TrustChainHelper
import nl.tudelft.trustchain.common.util.VotingHelper
import nl.tudelft.trustchain.common.util.VotingMode
import org.json.JSONException
import org.json.JSONObject

class VotingActivity : AppCompatActivity() {

    private lateinit var vh: VotingHelper
    private lateinit var community: VotingCommunity
    private lateinit var adapter: BlockListAdapter
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

        blockList.layoutManager = LinearLayoutManager(this)

        adapter = BlockListAdapter(voteProposals, vh)

        adapter.onItemClick = { block ->
            try {
                showNewCastVoteDialog(block)
                showVoteCompletenessToast(block)
            } catch (e: Exception) {
                printShortToast(e.message.toString())
            }
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
    @SuppressLint("InflateParams")
    private fun showNewVoteDialog() {

        val dialogView = LayoutInflater.from(this).inflate(R.layout.initiate_dialog, null)

        val builder = AlertDialog.Builder(this)
            .setView(dialogView)
            .setTitle("Initiate vote on proposal")

        val switch = dialogView.findViewById<Switch>(R.id.votingModeToggle)
        val switchLabel = dialogView.findViewById<TextView>(R.id.votingMode)
        switchLabel.text = getString(R.string.yes_no_mode)

        var votingMode = VotingMode.YESNO

        switch.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                switchLabel.text = getString(R.string.threshold_mode)
                votingMode = VotingMode.THRESHOLD
            } else {
                switchLabel.text = getString(R.string.yes_no_mode)
                votingMode = VotingMode.YESNO
            }
        }

        builder.setPositiveButton("Create") { _, _ ->

            val proposal = dialogView.findViewById<EditText>(R.id.proposalInput).text.toString()

            // Create list of your peers and include yourself
            val peers: MutableList<PublicKey> = ArrayList()
            peers.addAll(community.getPeers().map { it.publicKey })
            peers.add(community.myPeer.publicKey)

            // Start voting procedure
            vh.createProposal(proposal, peers, votingMode)
            printShortToast("Proposal has been created")
        }

        // NeutralButton is always the leftmost button
        builder.setNeutralButton("Cancel") { dialog, _ ->
            dialog.cancel()
        }

        builder.show()
    }

    /**
     * Make user aware of proposal status through toast message.
     */
    private fun showVoteCompletenessToast(block: TrustChainBlock) {
        val completed = vh.votingIsComplete(block, VotingCommunity.threshold)
        if (completed) {
            printShortToast("Voting has been completed.")
        } else {
            printShortToast("Voting still open.")
        }
    }

    /**
     * Show dialog from which the user can propose a vote
     */
    private fun showNewCastVoteDialog(block: TrustChainBlock) {
        val builder: AlertDialog.Builder = AlertDialog.Builder(this)

        // Parse the 'message' field as JSON.
        var voteSubject = ""
        var votingMode = VotingMode.YESNO
        try {
            val voteJSON = JSONObject(block.transaction["message"].toString())
            voteSubject = voteJSON.get("VOTE_SUBJECT").toString()
            votingMode = VotingMode.valueOf(voteJSON.get("VOTE_MODE").toString())
        } catch (e: JSONException) {
            "Block was a voting block but did not contain " +
                "proper JSON in its message field: ${block.transaction["message"]}."
        }

        // Convert block date to simpler format
        val date = DateFormat.format("EEE MMM d HH:mm", block.timestamp).toString()

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
            when (votingMode) {
                VotingMode.YESNO -> {
                    "<br><br>" +
                        "<small><b>You have voted</b>: <i>" +
                        hasCasted +
                        "</i></small>"
                }
                VotingMode.THRESHOLD -> {
                    "<br><br><small><b>You have voted</b>: <i>Agree</i></small>"
                }
            }
        } else {
            ""
        }

        // Get tally values
        val tally = getTally(voteSubject, block)

        // Show vote subject, proposer and current tally
        builder.setMessage(
            HtmlCompat.fromHtml(
                "<big>\"" + voteSubject + "\"</big>" +
                    "<br><br>" +
                    "<small><b>Proposed by</b>:" +
                    "<br>" +
                    "<i>" + defaultCryptoProvider.keyFromPublicBin(block.publicKey) + "</i></small>" +
                    "<br><br>" +
                    "<small><b>Date</b>: " +
                    "<i>" + date + "</i></small>" +
                    castedString +
                    "<br><br>" +
                    "<small><b>" + (
                    if (vh.votingIsComplete(
                            block,
                            VotingCommunity.threshold
                        )
                    ) "Final result" else "Current tally"
                    ) + "</b>:" +
                    "<br>" +
                    when (votingMode) {
                        VotingMode.YESNO -> {
                            "Yes votes: " + tally.first +
                                " | No votes: " + tally.second
                        }
                        VotingMode.THRESHOLD -> {
                            tally.first.toString() + " in favour"
                        }
                    } + "</small></i>",
                HtmlCompat.FROM_HTML_MODE_LEGACY
            )
        )

        // Display vote options is not previously casted a vote
        if (hasCasted == null) {
            builder.setTitle("Cast vote on proposal:")

            when (votingMode) {
                VotingMode.YESNO -> {
                    builder.setPositiveButton("YES") { _, _ ->
                        vh.respondToProposal(true, block)
                        printShortToast("You voted: YES")
                    }

                    builder.setNegativeButton("NO") { _, _ ->
                        vh.respondToProposal(false, block)
                        printShortToast("You voted: NO")
                    }
                }

                VotingMode.THRESHOLD -> {
                    builder.setPositiveButton("AGREE") { _, _ ->
                        vh.respondToProposal(true, block)
                        printShortToast("You voted: AGREE")
                    }
                }
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
    private fun getTally(voteSubject: String, block: TrustChainBlock): Pair<Int, Int> {
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
                proposalListUpdate()
                delay(1000)
            }
        }
    }

    /**
     * Update the list of proposals
     */
    private fun proposalListUpdate() {
        val currentProposals = tch.getBlocksByType("voting_block").filter {

            // Only the original proposal blocks.
            !JSONObject(it.transaction["message"].toString()).has("VOTE_REPLY") &&

                // Only those that you are eligible for
                vh.getVoters(it).any { key ->
                    key.pub().keyToBin()
                        .contentEquals(community.myPeer.publicKey.keyToBin())
                } &&

                // Take display mode into consideration
                displayBlock(
                    it
                )
        }.asReversed()

        // Update vote proposal set
        if (voteProposals != currentProposals) {
            voteProposals.clear()
            voteProposals.addAll(currentProposals)
        }

        adapter.notifyDataSetChanged()
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
