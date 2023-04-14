package nl.tudelft.trustchain.detoks

import android.app.AlertDialog
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.View
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
    private var tokenIDCounter = 0

    private val handler = Handler(Looper.getMainLooper())
    private val runnable = object : Runnable {
        override fun run() {
            updateList()
            handler.postDelayed(this, 1000)
        }
    }

    /**
     * Sends a single token to the selected peer
     */
    fun sendSingleToken(): Long {

        val executionTime = measureTimeMillis {
            transactionEngine.sendTokenSingle(
                transactionEngine.tokenStore.getSingleToken(),
                transactionEngine.getSelectedPeer()
            )

        }

        return executionTime
    }

    /**
     * Start benchmark for single token transactions
     * @param totalTransactions The total number of transactions to be sent
     */
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
                "DeToksTransactionEngine",
                "Batch of groups has been sent. Execution time: ${sendingTime} s"
            )
            delay(30 * 1000L)

        } while (transactionEngine.tokenStore.getAllTokens().isNotEmpty())

        val totalExecutionTime = (System.nanoTime() - startTime) / 1000000000
        Log.d(
            "DeToksTransactionEngine",
            "All tokens sent. Total Execution time: ${totalExecutionTime} s"
        )

        return totalExecutionTime
    }

    /**
     * Start benchmark for grouped transactions
     * @param totalTransactions The total number of transactions to be sent
     */
    suspend fun groupedBenchmark(): Long {
        val startTime = System.nanoTime()
        do {
            for (transactionGroup in transactionEngine.tokenStore.getAllTokens()
                .chunked(groupSize)) {
                transactionEngine.sendTokenGrouped(
                    listOf(transactionGroup),
                    transactionEngine.getSelectedPeer()
                )
            }
            val sendingTime = (System.nanoTime() - startTime) / 1000000
            Log.d(
                "DeToksTransactionEngine",
                "Batch of groups has been sent. Execution time: ${sendingTime} ms"
            )

            delay(30 * 1000L)
            Log.d(
                "DeToksTransactionEngine",
                "Current tokens in wallet: ${transactionEngine.tokenStore.getAllTokens()} ms"
            )
        } while (transactionEngine.tokenStore.getAllTokens().isNotEmpty())

        val totalExecutionTime = (System.nanoTime() - startTime) / 1000000000
        Log.d(
            "DeToksTransactionEngine",
            "All tokens sent. Total Execution time: ${totalExecutionTime} s"
        )
        return totalExecutionTime
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        // Get communities and services
        ipv8 = getIpv8()
        transactionEngine = ipv8.getOverlay()!!
        trustchainCommunity = ipv8.getOverlay()!!

        // Initialize the peer variables and set selected peer textview
        if (!transactionEngine.isPeerSelected()) {
            transactionEngine.initializePeers(ipv8.myPeer)
        }
        binding.otherPeers.text = connectedPeerToString()

        // Set button onclick listeners
        binding.generateTokensButton.setOnClickListener {
            generateTokens()
            updateList()
        }

        binding.startTransactionsButton.setOnClickListener {
            binding.transactionsPerSecondField.text = "${lifecycleScope.launch{groupedBenchmark()}} ms"
        }

        binding.singleTransactionsButton.setOnClickListener {
            binding.singleTextField.text = "${lifecycleScope.launch{singleBenchmark()}} ms"
        }

        binding.singleTokenButton.setOnClickListener{
            binding.singleTokenText.text ="${sendSingleToken()} ms"
        }

        binding.otherPeers.text = connectedPeerToString()
        binding.resetTokensButton.setOnClickListener {
            transactionEngine.tokenStore.removeAllTokens()
        }

        binding.selectReceiverButton.setOnClickListener {
            val navController = Navigation.findNavController(view)
            navController.navigate(R.id.action_connect_to_peer_fragment)
        }

        // Initialize listview adapter and set listview onclick listeners
        adapter = TokenAdapter(
            requireActivity(),
            transactionEngine.tokenStore.getAllTokens() as ArrayList<Token>
        )

        binding.tokenListView.isClickable = true
        binding.tokenListView.adapter = adapter

        binding.tokenListView.setOnItemClickListener() { _, _, position, _ ->
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
            updateList()
        }
    }

    /**
     * Updates the list of tokens
     */
    private fun updateList() {
        Log.d("UPDATE_VIEWS", "Updating List")
        adapter.clear()
        for (token in transactionEngine.tokenStore.getAllTokens()){
            adapter.insert(token, adapter.count)
        }
        binding.tokenAmount.text = transactionEngine.tokenStore.getBalance().toString() + " tokens"
    }

    /**
     * Generates tokens based on the totalTransactions variable and
     * adds them to local tokenStore
     * @param totalTransactions the amount of tokens to generate
     */
    private fun generateTokens() {
        for (i in 1..totalTransactions) {
            // Add Token to the token store
            transactionEngine.tokenStore.addToken(UUID.randomUUID().toString(), ipv8.myPeer.publicKey.keyToBin().toString(), i.toLong())
        }
    }

    /**
     * Returns the PublicKey string format of the connected peer
     * returns your own PublicKey if no peer is selected
     */
    private fun connectedPeerToString() : String {
        if (!transactionEngine.isPeerSelected()) {
            return transactionEngine.getSelfPeer().publicKey.toString()
        } else {
            return transactionEngine.getSelectedPeer().publicKey.toString()
        }
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
