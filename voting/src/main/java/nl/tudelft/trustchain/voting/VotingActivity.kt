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

    lateinit var vh: VotingHelper
    lateinit var community: TrustChainCommunity
    lateinit var adapter: blockListAdapter
    lateinit var tch: TrustChainHelper

    var voteProposals: MutableList<TrustChainBlock> = mutableListOf()
    var displayAllVotes: Boolean = true

    /**
     * Setup method, binds functionality
     */
    @SuppressLint("SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main_voting)

        initiateButton.setOnClickListener {
            showNewVoteDialog()
        }

        uncastedToggle.setOnCheckedChangeListener { _, isChecked ->
            displayAllVotes = if (isChecked) {
                proposalOverViewTitle.text = "New Votes"
                printShortToast("Displaying proposals to cast on")
                false
            } else {
                proposalOverViewTitle.text = "All Votes"
                printShortToast("Displaying all votes")
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
     * Dialog for a new proposal vote
     */
    private fun showNewVoteDialog() {
        val builder: AlertDialog.Builder = AlertDialog.Builder(this)
        builder.setTitle("New proposal vote")

        val input = EditText(this)
        input.inputType = InputType.TYPE_CLASS_TEXT
        input.hint = "p != np"
        builder.setView(input)

        builder.setPositiveButton("Create") { _, _ ->
            val proposal = input.text.toString()

            // Create list of your peers and include yourself
            val peers: MutableList<PublicKey> = ArrayList()
            peers.addAll(community.getPeers().map { it.publicKey })
            peers.add(community.myPeer.publicKey)

            // Start voting procedure
            vh.startVote(proposal, peers)
            printShortToast("Voting procedure started")
        }

        builder.setNegativeButton("Cancel") { dialog, _ ->
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
        val hasCasted = previouslyCastedVotes != Pair(0, 0)

        val castedString = if (hasCasted) {
            "<br><br>" +
                "<small><b>Your cast</b>: <i>" +
                previouslyCastedVotes.toString() +
                "</i></small>"
        } else {
            ""
        }

        builder.setMessage(
            Html.fromHtml(
                "<big>\"" + voteSubject + "\"</big>" +
                    "<br><br>" +
                    "<small><b>Proposed by</b>: <i>" +
                    defaultCryptoProvider.keyFromPublicBin(block.publicKey) +
                    "</i></small>" +
                    "<br><br>" +
                    "<small><b>Date</b>: <i>" +
                    block.timestamp +
                    "</i></small>" +
                    castedString, Html.FROM_HTML_MODE_LEGACY
            )
        )

        // Display vote options is not previously casted a vote
        if (!hasCasted) {
            builder.setTitle("Cast vote on proposal:")

            builder.setPositiveButton("YES") { _, _ ->
                vh.respondToVote(true, block)
                printShortToast("You voted: YES")
            }

            builder.setNegativeButton("NO") { _, _ ->
                vh.respondToVote(false, block)
                printShortToast("You voted: NO")
            }
            builder.setNeutralButton("CANCEL") { dialog, _ ->
                printShortToast("No vote was cast")
                dialog.cancel()
            }
        } else {
            builder.setTitle("Inspect proposal:")

            builder.setNeutralButton("Exit") { dialog, _ ->
                dialog.cancel()
            }
        }

        builder.setCancelable(true)

        builder.show()
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
