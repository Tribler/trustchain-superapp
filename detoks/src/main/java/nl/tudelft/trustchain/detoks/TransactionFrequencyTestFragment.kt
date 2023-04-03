package nl.tudelft.trustchain.detoks

import android.os.Bundle
import android.view.View
import android.widget.Button
import androidx.navigation.Navigation
import nl.tudelft.ipv8.IPv8
import nl.tudelft.ipv8.attestation.trustchain.TrustChainCommunity
import nl.tudelft.trustchain.common.ui.BaseFragment
import nl.tudelft.trustchain.common.util.viewBinding
import nl.tudelft.trustchain.detoks.databinding.FragmentTransactionfreqencyTestBinding
import kotlin.system.measureTimeMillis

class TransactionFrequencyTestFragment : BaseFragment(R.layout.fragment_transactionfreqency_test) {

    private val binding by viewBinding(FragmentTransactionfreqencyTestBinding::bind)

    private lateinit var ipv8: IPv8
    private lateinit var transactionEngine: DeToksTransactionEngine

    private lateinit var trustchainCommunity: TrustChainCommunity
    private val BLOCK_TYPE = "our_test_block"
    private val BLOCK_TYPE2 = "group_test_block"
    private val BLOCK_TYPE3 = "total_test_block"

    private var transaction_index = 0
    private var totalTransactions = 100
    private var groupSize = 10
    private var tokensPerTransaction = 1

    private val dummyTokens = mutableListOf<Token>()
    private val dummyTransactions = mutableListOf<List<Token>>()

    fun singleBenchmark(): Long {

        val executionTime = measureTimeMillis {
            for (tok in dummyTokens) {
                transactionEngine.sendTokenSingle(tok, ipv8.myPeer)
            }
        }

        return executionTime
    }

    fun groupedBenchmark(): Long {

        val executionTime = measureTimeMillis {

            for (transactionGroup in dummyTransactions.chunked(groupSize)) {

            }

                transactionEngine.sendTokenGrouped(dummyTransactions, ipv8.myPeer,groupSize)

//            for (i in 0..(totalTransactions / groupSize) - 1) {
//
//                val transactionList: MutableList<Map<String, Any>> = mutableListOf()
//                for (j in 0..groupSize - 1) {
//                    val transaction = mapOf("halfblock" to j, "peer_id" to i)
//                    transactionList.add(transaction)
//                }
//
//                val transaction = mapOf(
//                    "proposal" to transaction_index,
//                    "peer_id" to 1,
//                    "transactionList" to transactionList
//                )
//
//                // To show the transactions on screen
////                val builder = AlertDialog.Builder(requireContext())
////                builder.setTitle("Test")
////                builder.setMessage("Halfblock: ${transaction}")
////                builder.setNeutralButton("OK") { _, _ -> }
////                val dialog = builder.create()
////                dialog.show()
//
//                trustchainCommunity.createProposalBlock(
//                    BLOCK_TYPE2,
//                    transaction,
//                    ipv8.myPeer.publicKey.keyToBin()
//                )
//
//                transaction_index += 1
//
//
//            }
        }
        return executionTime
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {

        // get communities and services
        ipv8 = getIpv8()
        transactionEngine = ipv8.getOverlay()!!
        trustchainCommunity = ipv8.getOverlay()!!

        // populate the dummy tokens list
        for (i in 0..totalTransactions) {
            dummyTokens.add(Token("abc", ipv8.myPeer.publicKey.keyToBin()))
        }

        // populate the dummy transactions
        repeat(totalTransactions / groupSize) {
            // create tokens
            val toks = mutableListOf<Token>()
            repeat(tokensPerTransaction) {
                toks.add(Token("abc", ipv8.myPeer.publicKey.keyToBin()))
            }
            dummyTransactions.add(toks)
        }

        val environmentSwitchButton = view.findViewById<Button>(R.id.switch_environment_button)
        environmentSwitchButton.setOnClickListener { switchEnvirmonments(view) }

        val toTestButton = view.findViewById<Button>(R.id.toTest_button)
        toTestButton.setOnClickListener { toTest(view) }

        binding.startTransactionsButton.setOnClickListener {
            binding.transactionsPerSecondField.text = "${groupedBenchmark()} ms"
        }

        binding.singleTransactionsButton.setOnClickListener {
            binding.singleTextField.text = "${singleBenchmark()} ms"
        }

//        trustchainCommunity.addListener(BLOCK_TYPE2, object : BlockListener {
//            override fun onBlockReceived(block: TrustChainBlock) {
//                if (block.isProposal) {
//                    val transaction = mapOf(
//                        "agreement" to block.transaction["proposal"],
//                        "peer_id" to block.transaction["peer_id"],
//                        "transactionList" to block.transaction["transactionList"]
//                    )
//                    trustchainCommunity.createAgreementBlock(
//                        block,
//                        transaction
//                    )
//
//                    val builder = AlertDialog.Builder(requireContext())
//                    builder.setTitle("Test")
//                    builder.setMessage("Halfblock: ${transaction}")
//                    builder.setNeutralButton("OK") { _, _ -> }
//                    val dialog = builder.create()
//                    dialog.show()
//                }
//                if (block.isAgreement) {
//                    print("Agreement reached")
//                }
//                transaction_index
//            }
//        })
//
//        trustchainCommunity.addListener(BLOCK_TYPE3, object : BlockListener {
//            override fun onBlockReceived(block: TrustChainBlock) {
//                if (block.isProposal) {
//                    val transaction = mapOf(
//                        "agreement" to block.transaction["proposal"],
//                        "peer_id" to block.transaction["peer_id"]
//                    )
//                    trustchainCommunity.createAgreementBlock(
//                        block,
//                        transaction
//                    )
//                }
//                if (block.isAgreement) {
//                    print("Agreement reached")
//                }
//                transaction_index
//            }
//        })
    }

    fun switchEnvirmonments(view: View) {
        val navController = Navigation.findNavController(view)
        navController.navigate(R.id.action_switch_environment)
    }

    fun toTest(view: View) {
        val navController = Navigation.findNavController(view)
        navController.navigate(R.id.action_to_test)
    }
}
