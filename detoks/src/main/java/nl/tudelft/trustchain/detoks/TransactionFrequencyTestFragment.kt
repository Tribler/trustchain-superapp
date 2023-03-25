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
import kotlin.system.measureTimeMillis


class TransactionFrequencyTestFragment : BaseFragment(R.layout.fragment_transactionfreqency_test) {

    private val binding by viewBinding(FragmentTransactionfreqencyTestBinding::bind)

    private lateinit var ipv8: IPv8
    private lateinit var community: OurCommunity
    private lateinit var trustchainCommunity: TrustChainCommunity
    private val BLOCK_TYPE = "our_test_block"
    private val BLOCK_TYPE2 = "group_test_block"

    private var transaction_index = 0;
    private val count = 0

//    private fun generateTokensFor1Sec(): Int {
//        val startTime = System.currentTimeMillis()
//        val endTime = startTime + 1000 // 1 second time limit
//        var numTokens = 0
//
//        while (System.currentTimeMillis() < endTime) {
//            val unique_id = "token-$numTokens"
//            val token = Token(unique_id)
//            token.serialize()
//            numTokens++
//        }
//
//        return numTokens
//    }


    private fun grouppacking(): Long {
        val executionTime = measureTimeMillis {
            for (i in 0..9) {
                val halfblocks = mutableListOf<ByteArray>()
                for (j in 0..9) {
                    val transaction = mapOf("halfblock" to j, "peer_id" to i)
                    val token = Token("transaction_${i}_$j", ipv8.myPeer.publicKey.keyToBin())
                    val serialized_token = token.serialize()
                    halfblocks.add(serialized_token + transaction.toString().toByteArray())
                }
                // Map the 10 half blocks to form a list of full blocks
                //            val blocks = halfblocks
                //                .take(10)
                //                .zip(halfblocks.takeLast(10))
                //                .map { (firstHalf, secondHalf) -> firstHalf + secondHalf }
                val block = halfblocks.reduce { acc, byteArray -> acc + byteArray }

                val propose_transaction = mapOf("proposal" to i, "block" to block)
                trustchainCommunity.createProposalBlock(
                    BLOCK_TYPE2,
                    propose_transaction,
                    ipv8.myPeer.publicKey.keyToBin()
                )

            }
        }
        return executionTime
    }
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {

        community = IPv8Android.getInstance().getOverlay()!!
        trustchainCommunity = IPv8Android.getInstance().getOverlay()!!
        ipv8 = IPv8Android.getInstance()


        val environmentSwitchButton = view.findViewById<Button>(R.id.switch_environment_button)
        environmentSwitchButton.setOnClickListener { switchEnvirmonments(view) }

        binding.startTransactionsButton.setOnClickListener {
            binding.transactionsPerSecondField.text = "${grouppacking()} ms"
        }
        trustchainCommunity.addListener(BLOCK_TYPE2, object : BlockListener {
            override fun onBlockReceived(block: TrustChainBlock) {
                if (block.isProposal){
                    trustchainCommunity.createAgreementBlock(block, mapOf("agreement" to block.transaction["proposal"], "block_id" to block.transaction["block"]))
                }
                if(block.isAgreement){
                    print("Agreement reached")
                }
            } })
    }
    fun switchEnvirmonments(view: View){
        val navController = Navigation.findNavController(view)
        navController.navigate(R.id.action_switch_environment)
    }



}

