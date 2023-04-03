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
class BenchmarkFragment : BaseFragment(R.layout.fragment_transactionfreqency_test) {

    private val binding by viewBinding(FragmentTransactionfreqencyTestBinding::bind)

    private lateinit var ipv8: IPv8
    private lateinit var transactionEngine: DeToksTransactionEngine

    private lateinit var trustchainCommunity: TrustChainCommunity

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
                transactionEngine.sendTokenGrouped(transactionGroup, ipv8.myPeer)
            }
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
        for(i in 0 .. totalTransactions){
            dummyTransactions.add(listOf(dummyTokens[i]))
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
