package nl.tudelft.trustchain.detoks

import android.app.AlertDialog
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.TextView
import androidx.navigation.Navigation
import androidx.navigation.Navigation.findNavController
import androidx.navigation.fragment.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.NavigationUI.setupActionBarWithNavController
import nl.tudelft.ipv8.IPv8
import nl.tudelft.ipv8.Peer
import nl.tudelft.ipv8.android.IPv8Android
import nl.tudelft.ipv8.attestation.trustchain.BlockListener
import nl.tudelft.ipv8.attestation.trustchain.TrustChainBlock
import nl.tudelft.ipv8.attestation.trustchain.TrustChainCommunity
import nl.tudelft.trustchain.common.ui.BaseFragment
import nl.tudelft.trustchain.common.util.viewBinding
import nl.tudelft.trustchain.detoks.databinding.FragmentExampleoverlayBinding
import nl.tudelft.trustchain.detoks.databinding.FragmentTransactionfreqencyTestBinding

class TransactionFrequencyTestFragment : BaseFragment(R.layout.fragment_transactionfreqency_test) {

    private val binding by viewBinding(FragmentTransactionfreqencyTestBinding::bind)

    private lateinit var ipv8: IPv8
    private lateinit var community: OurCommunity
    private lateinit var trustchainCommunity: TrustChainCommunity
    private val BLOCK_TYPE = "our_test_block"

    private var transaction_index = 0;

    private fun generateTokensFor1Sec(): Int {
        val startTime = System.currentTimeMillis()
        val endTime = startTime + 1000 // 1 second time limit
        var numTokens = 0

        while (System.currentTimeMillis() < endTime) {
            val unique_id = "token-$numTokens"
            val token = Token(unique_id)
            token.serialize()
            numTokens++
        }

        return numTokens
    }

    private fun createProposal(recipient: Peer, peer_id: Int) {
        val transaction = mapOf("proposal" to transaction_index, "peer_id" to peer_id)
        trustchainCommunity.createProposalBlock(
            BLOCK_TYPE,
            transaction,
            recipient.publicKey.keyToBin()
        )
        community.addTransaction(
            transaction_index,
            ipv8.myPeer.publicKey.toString(),
            recipient.publicKey.toString(),
            "proposal"
        )
        transaction_index += 1
    }


    private fun createAgreement(block: TrustChainBlock) {
        val transaction = mapOf(
            "agreement" to block.transaction["proposal"],
            "peer_id" to block.transaction["peer_id"]
        )
        trustchainCommunity.createAgreementBlock(
            block,
            transaction,
        )
        // TODO - I increment transaction index here to keep unique IDs in the DB
        community.addTransaction(
            transaction_index,
            ipv8.myPeer.publicKey.toString(),
            block.transaction["peer_id"].toString(),
            "agreement"
        )
        transaction_index += 1
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {

        community = IPv8Android.getInstance().getOverlay()!!
        trustchainCommunity = IPv8Android.getInstance().getOverlay()!!
        ipv8 = IPv8Android.getInstance()

        // Reset DB
        community.resetDatabase()

        val environmentSwitchButton = view.findViewById<Button>(R.id.switch_environment_button)
        environmentSwitchButton.setOnClickListener { switchEnvirmonments(view) }

        binding.startTransactionsButton.setOnClickListener {
            binding.transactionsPerSecondField.text =
                "${generateTokensFor1Sec()} transactions per second"
        }
    }
    fun switchEnvirmonments(view: View){
        val navController = Navigation.findNavController(view)
        navController.navigate(R.id.action_switch_environment)
    }



}

