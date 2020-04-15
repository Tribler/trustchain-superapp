package nl.tudelft.trustchain.voting

import android.app.AlertDialog
import android.os.Bundle
import android.text.InputType
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import kotlinx.android.synthetic.main.activity_main_voting.*
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
    lateinit var voteProposals: List<TrustChainBlock>
    lateinit var tch: TrustChainHelper

    /**
     * Setup method, binds functionality
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main_voting)

        initiateButton.setOnClickListener {
            showNewVoteDialog()
        }

        val ipv8 = IPv8Android.getInstance()

        // Initiate community and helpers
        community = ipv8.getOverlay()!!
        vh = VotingHelper(community)
        tch = TrustChainHelper(community)

        blockList.addItemDecoration(DividerItemDecoration(this, LinearLayoutManager.VERTICAL))
        blockList.layoutManager = LinearLayoutManager(this)

        voteProposals = tch.getBlocksByType("voting_block").filter {
            !JSONObject(it.transaction["message"].toString()).has("VOTE_REPLY")
        }

        adapter = blockListAdapter(voteProposals)

        adapter.onItemClick = {
            showNewCastVoteDialog(it)
        }

        blockList.adapter = adapter
    }

    /**
     * Display a short message on the screen
     */
    private fun printShortToast(s: String) {
        Toast.makeText(this, s, Toast.LENGTH_LONG).show()
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

            // Update list
            updateVoteProposalList()
        }

        builder.setNegativeButton("Cancel") { dialog, _ ->
            dialog.cancel()
        }

        builder.show()
    }

    /**
     * Refresh the vote proposals
     */
    private fun updateVoteProposalList() {
        voteProposals = tch.getBlocksByType("voting_block").filter {
            !JSONObject(it.transaction["message"].toString()).has("VOTE_REPLY")
        }
        adapter.notifyDataSetChanged()
    }

    /**
     * Show dialog from which the user can propose a vote
     */
    private fun showNewCastVoteDialog(block: TrustChainBlock) {
        val builder: AlertDialog.Builder = AlertDialog.Builder(this)
        builder.setTitle("Cast vote on proposal:")

        // Parse the 'message' field as JSON.
        var voteSubject = ""
        try {
            val voteJSON = JSONObject(block.transaction["message"].toString())
            voteSubject = voteJSON.get("VOTE_SUBJECT").toString()
        } catch (e: JSONException) {
            "Block was a voting block but did not contain " +
                "proper JSON in its message field: ${block.transaction["message"]}."
        }

        builder.setMessage(voteSubject + "\n\n" + "Proposed by: " + defaultCryptoProvider.keyFromPublicBin(block.publicKey))

        builder.setPositiveButton("YES") { _, _ ->
            vh.respondToVote(true, block)
            printShortToast("You voted: YES")
        }

        builder.setNegativeButton("NO") { _, _ ->
            vh.respondToVote(false, block)
            printShortToast("You voted: NO")
        }

        builder.setNeutralButton("CANCEL") {dialog, _ ->
            printShortToast("No vote was cast")
            dialog.cancel()
        }

        builder.setCancelable(true)

        builder.show()
    }
}
