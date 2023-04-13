package nl.tudelft.trustchain.detoks

import android.app.AlertDialog
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.View
import android.widget.Button
import androidx.lifecycle.lifecycleScope
import androidx.navigation.Navigation
import nl.tudelft.ipv8.IPv8
import nl.tudelft.ipv8.attestation.trustchain.TrustChainCommunity
import nl.tudelft.trustchain.common.ui.BaseFragment
import nl.tudelft.trustchain.common.util.viewBinding
import nl.tudelft.trustchain.detoks.databinding.FragmentBenchmarkBinding
import java.util.UUID
import kotlin.system.measureTimeMillis
import kotlinx.coroutines.*

class BenchmarkFragment : BaseFragment(R.layout.fragment_benchmark) {

    private val binding by viewBinding(FragmentBenchmarkBinding::bind)

    private lateinit var ipv8: IPv8
    private lateinit var transactionEngine: DeToksTransactionEngine
    private lateinit var trustchainCommunity: TrustChainCommunity
    private lateinit var adapter: TokenAdapter

    private var totalTransactions = 1000
    private var groupSize = 100
    private var tokensPerTransaction = 1
    private var tokenIDCounter = 0

    private val handler = Handler(Looper.getMainLooper())
    private val runnable = object : Runnable {
        override fun run() {
            updateList()
            handler.postDelayed(this, 1000)
        }
    }

    suspend fun singleBenchmark(): Long {

        val startTime = System.nanoTime()
        var tokenList : ArrayList<Token>
        do {
            tokenList = transactionEngine.tokenStore.getAllTokens() as ArrayList<Token>
            for (tok in tokenList){
                transactionEngine.sendTokenSingle(tok, transactionEngine.getSelectedPeer())
            }
            val sendingTime = (System.nanoTime() - startTime) / 1000000
            Log.d(
                "BENCHMARK",
                "Batch of groups has been sent. Execution time: ${sendingTime} s"
            )
            delay(30 * 1000L)

        } while (transactionEngine.tokenStore.getAllTokens().isNotEmpty())

        val totalExecutionTime = (System.nanoTime() - startTime) / 1000000000
        Log.d(
            "BENCHMARK",
            "All tokens sent. Total Execution time: ${totalExecutionTime} s"
        )

        return totalExecutionTime
    }

    suspend fun groupedBenchmark(): Long {
        val startTime = System.nanoTime()
        var tokenList: ArrayList<Token>
        do {
            tokenList = transactionEngine.tokenStore.getAllTokens() as ArrayList<Token>
            Log.d("BENCHMARK", "Sending ${tokenList.size} tokens. List: ${tokenList}")
            for (transactionGroup in transactionEngine.tokenStore.getAllTokens()
                .chunked(groupSize)) {
                transactionEngine.sendTokenGrouped(
                    listOf(transactionGroup),
                    transactionEngine.getSelectedPeer()
                )
            }
            val sendingTime = (System.nanoTime() - startTime) / 1000000
            Log.d(
                "BENCHMARK",
                "Batch of groups has been sent. Execution time: ${sendingTime} s"
            )

            delay(30 * 1000000L)
            Log.d(
                "BENCHMARK",
                "Remaining group ${transactionEngine.tokenStore.getAllTokens().size}. List: ${transactionEngine.tokenStore.getAllTokens()}"
            )

        } while (transactionEngine.tokenStore.getAllTokens().isNotEmpty())

        val totalExecutionTime = (System.nanoTime() - startTime) / 1000000000
        Log.d(
            "BENCHMARK",
            "All tokens sent. Total Execution time: ${totalExecutionTime} s"
        )

        return totalExecutionTime
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {

        // get communities and services
        ipv8 = getIpv8()
        transactionEngine = ipv8.getOverlay()!!
        trustchainCommunity = ipv8.getOverlay()!!

        // initialize the peers
        if (!transactionEngine.isPeerSelected()) {
            transactionEngine.initializePeers(ipv8.myPeer)
        }

        binding.otherPeers.text = connectedPeerToString()

        binding.generateTokensButton.setOnClickListener {
            generateTokens()
        }

        val environmentSwitchButton = view.findViewById<Button>(R.id.switch_environment_button)
        environmentSwitchButton.setOnClickListener { switchEnvirmonments(view) }

        val toTestButton = view.findViewById<Button>(R.id.toTest_button)
        toTestButton.setOnClickListener { toTest(view) }

        binding.startTransactionsButton.setOnClickListener {
            binding.transactionsPerSecondField.text = "${lifecycleScope.launch{groupedBenchmark()}} ms"
        }

        binding.singleTransactionsButton.setOnClickListener {
            binding.singleTextField.text = "${lifecycleScope.launch{singleBenchmark()}} ms"
        }

        binding.otherPeers.text = connectedPeerToString()

        adapter = TokenAdapter(
            requireActivity(),
            transactionEngine.tokenStore.getAllTokens() as ArrayList<Token>
        )

        binding.blockListview.isClickable = true
        binding.blockListview.adapter = adapter

        binding.blockListview.setOnItemClickListener() { _, _, position, _ ->
            val token = adapter.getItem(position)
            val builder = AlertDialog.Builder(requireContext())
            builder.setTitle("Token ${token.unique_id}")
            builder.setMessage("Sender: ${token.public_key}")
            val dialog= builder.create()
            dialog.show()
        }

        binding.selectReceiverButton.setOnClickListener {
            val navController = Navigation.findNavController(view)
            navController.navigate(R.id.action_connect_to_peer_fragment)
        }

        binding.resetTokensButton.setOnClickListener {
            transactionEngine.tokenStore.removeAllTokens()
        }
    }

    private fun updateList() {
        Log.d("UPDATE_VIEWS", "Updating List")
        adapter.clear()
        for (token in transactionEngine.tokenStore.getAllTokens()){
            adapter.insert(token, adapter.count)
        }
        binding.tokenAmount.text = transactionEngine.tokenStore.getBalance().toString() + " tokens"
    }

    private fun generateTokens() {
        for (i in 1..totalTransactions) {
            // Add Token to the token store
            transactionEngine.tokenStore.addToken(UUID.randomUUID().toString(), ipv8.myPeer.publicKey.keyToBin().toString(), i.toLong())
            tokenIDCounter++
        }
    }

    private fun connectedPeerToString() : String {
        if (!transactionEngine.isPeerSelected()) {
            return transactionEngine.getSelfPeer().publicKey.toString()
        } else {
            return transactionEngine.getSelectedPeer().publicKey.toString()
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

    override fun onResume() {
        super.onResume()
        Log.d("UPDATE_VIEWS", "Handler set")
        handler.postDelayed(runnable, 1000)
    }

    override fun onPause() {
        super.onPause()
        Log.d("UPDATE_VIEWS", "Handler stopped")
        handler.removeCallbacks(runnable)
    }
}
