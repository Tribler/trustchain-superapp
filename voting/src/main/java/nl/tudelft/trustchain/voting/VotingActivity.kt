package nl.tudelft.trustchain.voting

import android.app.AlertDialog
import android.os.Bundle
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

    /**
     * Setup method, binds functionality
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main_voting)

        initiateButton.setOnClickListener {
            showNewVoteDialog()
        }

        // Initiate community and helpers
        val ipv8 = IPv8Android.getInstance()
        community = ipv8.getOverlay()!!
        vh = VotingHelper(community)
        tch = TrustChainHelper(community)

        // Stying
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
    fun printToast(s: String) {
        Toast.makeText(applicationContext, s, Toast.LENGTH_LONG).show()
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
            Toast.makeText(this, "Voting procedure started", Toast.LENGTH_SHORT).show()
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
        builder.setTitle("Cast vote")

        // Get the vote subject from the proposal.
        val voteSubject: String = try {
            // Parse the JSON object in the transaction's 'message' field.
            val voteJSON = JSONObject(block.transaction["message"].toString())

            // Retrieve the vote subject.
            voteJSON.get("VOTE_SUBJECT").toString()
        } catch (e: JSONException) {
            "Wrongly formatted vote request."
        }

        builder.setMessage(voteSubject)

        builder.setPositiveButton("YES") { _, _ ->
            vh.respondToVote(true, block)
        }

        builder.setNegativeButton("NO") { dialog, _ ->
            vh.respondToVote(false, block)
            dialog.cancel()
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
                    !JSONObject(it.transaction["message"].toString()).has("VOTE_REPLY")
                }.asReversed()

                // Update vote proposal set
                if (!voteProposals.equals(currentProposals)) {
                    voteProposals.clear()
                    voteProposals.addAll(tch.getBlocksByType("voting_block").filter {
                        !JSONObject(it.transaction["message"].toString()).has("VOTE_REPLY")
                    }.asReversed())
                    adapter.notifyDataSetChanged()
                }

                delay(1000)
            }
        }
    }
}
