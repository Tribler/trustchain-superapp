package nl.tudelft.trustchain.detoks

import android.app.AlertDialog
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import androidx.navigation.Navigation
import nl.tudelft.ipv8.IPv8
import nl.tudelft.ipv8.attestation.trustchain.TrustChainCommunity
import nl.tudelft.trustchain.common.ui.BaseFragment
import nl.tudelft.trustchain.common.util.viewBinding
import nl.tudelft.trustchain.detoks.databinding.FragmentBenchmarkBinding
import kotlin.system.measureTimeMillis

class BenchmarkFragment : BaseFragment(R.layout.fragment_benchmark) {

    private val binding by viewBinding(FragmentBenchmarkBinding::bind)

    private lateinit var ipv8: IPv8
    private lateinit var transactionEngine: DeToksTransactionEngine
    private lateinit var trustchainCommunity: TrustChainCommunity
    private lateinit var adapter: TokenAdapter

    private var totalTransactions = 1000
    private var groupSize = 10
    private var tokenIDCounter = 0

    /**
     * Start benchmark for grouped transactions
     * @param totalTransactions The total number of transactions to be sent
     */
    fun singleBenchmark(): Long {
        val executionTime = measureTimeMillis {
            for (tok in transactionEngine.tokenStore.getAllTokens()) {
                transactionEngine.sendTokenSingle(tok, transactionEngine.getSelectedPeer())
            }
        }

        return executionTime
    }

    /**
     * Start benchmark for grouped transactions
     * @param totalTransactions The total number of transactions to be sent
     */
    fun groupedBenchmark(): Long {
        val executionTime = measureTimeMillis {
            for (transactionGroup in transactionEngine.tokenStore.getAllTokens().chunked(groupSize)) {
                transactionEngine.sendTokenGrouped(listOf(transactionGroup), transactionEngine.getSelectedPeer())
            }
        }
        return executionTime
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
        }

        binding.startTransactionsButton.setOnClickListener {
            binding.transactionsPerSecondField.text = "${groupedBenchmark()} ms"
        }

        binding.singleTransactionsButton.setOnClickListener {
            binding.singleTextField.text = "${singleBenchmark()} ms"
        }

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

        // Update the listview every second
        val handler = Handler(Looper.getMainLooper())
        handler.postDelayed(object : Runnable {
            override fun run() {
                updateList()
                handler.postDelayed(this, 1000)
            }
        }, 1000)
    }

    /**
     * Updates the list of tokens
     */
    private fun updateList() {
        adapter.clear()
        for (token in transactionEngine.tokenStore.getAllTokens()){
            adapter.insert(token, adapter.count)
        }
    }

    /**
     * Generates tokens based on the totalTransactions variable and
     * adds them to local tokenStore
     * @param totalTransactions the amount of tokens to generate
     */
    private fun generateTokens() {
        for (i in 1..totalTransactions) {
            transactionEngine.tokenStore.addToken(tokenIDCounter.toString(), ipv8.myPeer.publicKey.keyToBin().toString())
            tokenIDCounter++
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
}
