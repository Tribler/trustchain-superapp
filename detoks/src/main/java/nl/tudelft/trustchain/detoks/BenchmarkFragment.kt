package nl.tudelft.trustchain.detoks

import android.app.AlertDialog
import android.icu.util.MeasureUnit.MEGABYTE
import android.os.Bundle
import android.os.Debug
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
    private val LOGTAG = "DeToksTransactionEngine"
    private val BENCHMARKTAG = "BenchmarkTransactions"
    private val MILLISECOND = 1000000
    private val SECOND = 1000000000
    var startBenchmark = 0L
    var benchMarking = ""
    private var totalTransactions = 1000
    private var groupSize = 10

    // Looper for updating the list of tokens
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
    fun sendSingleToken() {
        this.startBenchmark = System.nanoTime()
        this.benchMarking = "SingleToken"

        transactionEngine.sendTokenSingle(
            transactionEngine.tokenStore.getSingleToken(),
            transactionEngine.getSelectedPeer(),
            false
        )
        return
    }

    /**
     * Start benchmark for single token transactions
     * @param totalTransactions The total number of transactions to be sent
     */
    private suspend fun singleBenchmark() {
        this.startBenchmark = System.nanoTime()
        this.benchMarking = "SingleBatch"
        for (tok in transactionEngine.tokenStore.getAllTokens()){
            transactionEngine.sendTokenSingle(tok, transactionEngine.getSelectedPeer(), resend=false)
        }
        delay(5000L)
        while(transactionEngine.tokenStore.getAllTokens().isNotEmpty()){
            for (tok in transactionEngine.tokenStore.getAllTokens()){
                transactionEngine.sendTokenSingle(tok, transactionEngine.getSelectedPeer(), resend=true)
            }
        }
        return
    }

    /**
     * Start benchmark for grouped transactions
     * @param totalTransactions The total number of transactions to be sent
     */
    private suspend fun groupedBenchmark() {
        this.startBenchmark = System.nanoTime()
        this.benchMarking = "GroupedBatch"

        for (transactionGroup in transactionEngine.tokenStore.getAllTokens()
            .chunked(groupSize)) {
            transactionEngine.sendTokenGrouped(
                listOf(transactionGroup),
                transactionEngine.getSelectedPeer(),
                resend = false
            )
        }
        Log.d(BENCHMARKTAG, "Finished sending first batch, now resending=true")
        while(transactionEngine.tokenStore.getAllTokens().isNotEmpty()){
            for (transactionGroup in transactionEngine.tokenStore.getAllTokens()
                .chunked(groupSize)) {
                transactionEngine.sendTokenGrouped(
                    listOf(transactionGroup),
                    transactionEngine.getSelectedPeer(),
                    resend = true
                )
            }
        }
        return
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        Debug.startMethodTracing("benchMark2", 1024*128*128);

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
            saveSettings()
            adapter.setGroupSize(this.groupSize)
            generateTokens()
            updateList()
        }

        initializeSelectors()

        binding.startTransactionsButton.setOnClickListener {
            lifecycleScope.launch{groupedBenchmark()}
        }

        binding.singleTransactionsButton.setOnClickListener {
            lifecycleScope.launch{singleBenchmark()}
        }

        binding.singleTokenButton.setOnClickListener{
            sendSingleToken()
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
            builder.setTitle("Token ${token.tokenIntId}")
            builder.setMessage("UUID: ${token.unique_id}")
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
        Debug.stopMethodTracing()
    }

    /**
     * Initializes the transaction & grouping amount selectors and submit button.
     */
    private fun initializeSelectors() {
        val submitButton = binding.submitSettings
        submitButton.setOnClickListener {
            saveSettings()
        }
    }

    private fun saveSettings(){
        val transactionAmountSelector = binding.transactionAmountSelector
        val groupingAmountSelector = binding.groupingAmountSelector
        val transactionAmount = transactionAmountSelector.text.toString().toInt()
        val groupingAmount = groupingAmountSelector.text.toString().toInt()
        // Both values should be positive and the grouping amount should be lower than the transaction amount
        if (transactionAmount > 0 && groupingAmount > 0 && groupingAmount < transactionAmount) {
            totalTransactions = transactionAmount
            groupSize = groupingAmount
        } else {
            val builder = AlertDialog.Builder(requireContext())
            builder.setTitle("Invalid settings")
            builder.setMessage("Please make sure that the grouping amount is smaller than the transaction amount and that both amounts are larger than 0.")
            val dialog= builder.create()
            dialog.show()
        }
    }

    /**
     * Updates the list of tokens
     */
    private fun updateList() {
        adapter.clear()
        var tokenList = transactionEngine.tokenStore.getAllTokens() as ArrayList<Token>
        Log.d(
            LOGTAG,
            "groupSize: ${this.groupSize}"
        )
        if (benchMarking == "SingleToken"){
            val endTime = (System.nanoTime() - this.startBenchmark) / MILLISECOND
            binding.singleTokenText.text = "${endTime} ms"
            Log.d(
                LOGTAG,
                "TokenList is empty, totalTime: ${transactionEngine.totalTimeTracker / MILLISECOND} ms"
            )
            benchMarking = ""
        }

        if (tokenList.isNotEmpty()){
            for (token in tokenList){
                adapter.insert(token, adapter.count)
            }
        }
        else {

            val endTime = (System.nanoTime() - this.startBenchmark) / MILLISECOND
            if (benchMarking == "SingleBatch"){
                binding.singleTextField.text = "${endTime} ms"
                Log.d(
                    BENCHMARKTAG,
                    "SingleBatch processing time: ${transactionEngine.totalTimeTracker / MILLISECOND} ms"
                )
                Log.d(
                    BENCHMARKTAG,
                    "SingleBatch execution time: ${endTime} ms"
                )

            } else if (benchMarking == "GroupedBatch"){
                binding.transactionsPerSecondField.text = "${endTime} ms"
                Log.d(
                    BENCHMARKTAG,
                    "GroupedBatch processing time: ${transactionEngine.totalTimeTracker / MILLISECOND} ms"
                )
                Log.d(
                    BENCHMARKTAG,
                    "GroupedBatch execution time: ${endTime} ms"
                )

            }
            benchMarking = ""
            transactionEngine.totalTimeTracker = 0L
        }
        binding.tokenAmount.text = transactionEngine.tokenStore.getBalance().toString() + " tokens"
    }

    /**
     * Generates tokens based on the totalTransactions variable and
     * adds them to local tokenStore
     * @param totalTransactions the amount of tokens to generate
     */
    private fun generateTokens() {
        var uuidList = mutableListOf<String>()
        for (i in 1..totalTransactions) {
            // Add Token to the token store
            val uuid = UUID.randomUUID().toString()
            uuidList.add(uuid)
            transactionEngine.tokenStore.addToken(uuid, i.toLong())
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
        handler.postDelayed(runnable, 1000)
    }

    override fun onPause() {
        super.onPause()
        handler.removeCallbacks(runnable)
    }
}
