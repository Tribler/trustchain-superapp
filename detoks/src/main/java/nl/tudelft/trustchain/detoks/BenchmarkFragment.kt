package nl.tudelft.trustchain.detoks

import android.app.AlertDialog
import android.os.Bundle
import android.view.View
import android.widget.Button
import androidx.navigation.Navigation
import nl.tudelft.ipv8.IPv8
import nl.tudelft.ipv8.attestation.trustchain.TrustChainCommunity
import nl.tudelft.ipv8.attestation.trustchain.*
import nl.tudelft.trustchain.common.ui.BaseFragment
import nl.tudelft.trustchain.common.util.viewBinding
import nl.tudelft.trustchain.detoks.databinding.FragmentBenchmarkBinding
import kotlin.system.measureTimeMillis
class BenchmarkFragment : BaseFragment(R.layout.fragment_benchmark) {

    private val binding by viewBinding(FragmentBenchmarkBinding::bind)

    private lateinit var ipv8: IPv8
    private lateinit var transactionEngine: DeToksTransactionEngine
    private lateinit var trustchainCommunity: TrustChainCommunity
    private lateinit var adapter : TokenAdapter

    private var totalTransactions = 30
    private var groupSize = 10
    private var tokensPerTransaction = 1
    private var tokenIDCounter = 0

    private val dummyTokens = mutableListOf<Token>()
    private val dummyTransactions = mutableListOf<List<Token>>()

    fun singleBenchmark(): Long {

        val executionTime = measureTimeMillis {
            for (tok in dummyTokens) {
                transactionEngine.sendTokenSingle(tok, transactionEngine.getSelectedPeer())
            }
        }

        return executionTime
    }

    fun groupedBenchmark(): Long {

        val executionTime = measureTimeMillis {

            for (transactionGroup in dummyTransactions.chunked(groupSize)) {
                transactionEngine.sendTokenGrouped(transactionGroup, transactionEngine.getSelectedPeer())
            }
             }
        return executionTime
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {

        // get communities and services
        ipv8 = getIpv8()
        transactionEngine = ipv8.getOverlay()!!
        trustchainCommunity = ipv8.getOverlay()!!
        transactionEngine.tokenStore.removeAllTokens()

        if (!transactionEngine.isPeerSelected()) {
            transactionEngine.addPeer(ipv8.myPeer)
        }


        // populate the dummy tokens list
        for (i in 0..totalTransactions) {
            val token = Token(tokenIDCounter.toString(), ipv8.myPeer.publicKey.keyToBin())
            dummyTokens.add(token)
            // Add Token to the token store
            transactionEngine.tokenStore.addToken(tokenIDCounter.toString(), ipv8.myPeer.publicKey.keyToBin().toString())
            tokenIDCounter++
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

        binding.otherPeers.text = connectedPeerToString()

        adapter = TokenAdapter(requireActivity(),
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

        binding.updateListButton.setOnClickListener {
            adapter.clear()
            for (token in transactionEngine.tokenStore.getAllTokens()){
                adapter.insert(token, adapter.count)
            }
            binding.otherPeers.text = connectedPeerToString()
        }

        binding.clearReceiversButton.setOnClickListener {
            transactionEngine.addPeer(ipv8.myPeer)
            binding.otherPeers.text = connectedPeerToString()
        }

    }

    private fun connectedPeerToString() : String {
        return transactionEngine.getSelectedPeer().address.toString()
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
