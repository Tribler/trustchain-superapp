package nl.tudelft.trustchain.currencyii.ui.bitcoin

import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.forEach
import androidx.core.view.forEachIndexed
import androidx.lifecycle.lifecycleScope
import kotlinx.android.synthetic.main.fragment_join_network.*
import kotlinx.android.synthetic.main.join_sw_row_data.view.*
import kotlinx.coroutines.*
import nl.tudelft.trustchain.currencyii.CoinCommunity
import nl.tudelft.trustchain.currencyii.ui.BaseFragment
import nl.tudelft.ipv8.attestation.trustchain.TrustChainBlock
import nl.tudelft.ipv8.util.toHex
import nl.tudelft.trustchain.currencyii.R
import nl.tudelft.trustchain.currencyii.sharedWallet.SWJoinBlockTransactionData
import nl.tudelft.trustchain.currencyii.sharedWallet.SWSignatureAskBlockTD
import kotlin.concurrent.thread

/**
 * A simple [Fragment] subclass.
 * Use the [BitcoinFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class JoinNetworkFragment() : BaseFragment(R.layout.fragment_join_network) {
    private var adapter: SharedWalletListAdapter? = null
    private var fetchedWallets: ArrayList<TrustChainBlock> = ArrayList()
    private var isFetching: Boolean = false

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        initListeners()
        this.refresh()
    }

    private fun initListeners() {
        join_dao_refresh_swiper.setOnRefreshListener {
            this.refresh()
        }
    }

    private fun refresh() {
        enableRefresher()
        lifecycleScope.launchWhenStarted {
            fetchSharedWalletsAndUpdateUI()
        }
    }

    private fun enableRefresher() {
        try {
            this.isFetching = true
            join_dao_refresh_swiper.isRefreshing = true
        } catch (e: IllegalStateException) {
        }
    }

    private fun disableRefresher() {
        try {
            join_dao_refresh_swiper.isRefreshing = false
        } catch (e: IllegalStateException) {
        }
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        lifecycleScope.launchWhenStarted {
            fetchSharedWalletsAndUpdateUI()
        }
    }

    private fun fetchSharedWalletsAndUpdateUI() {
        lifecycleScope.launch {
            withContext(Dispatchers.IO) {
                val discoveredWallets = getCoinCommunity().discoverSharedWallets()
                updateSharedWallets(discoveredWallets)
                updateSharedWalletsUI()
                crawlAvailableSharedWallets()
                updateSharedWalletsUI()
            }
        }
    }

    private fun updateSharedWallets(newWallets: List<TrustChainBlock>) {
        val walletIds = fetchedWallets.map {
            SWJoinBlockTransactionData(it.transaction).getData().SW_UNIQUE_ID
        }
        val distinctById = newWallets
            .filter {
                // Make sure that the trust chain block has the correct type
                it.type == CoinCommunity.JOIN_BLOCK
            }.distinctBy {
                SWJoinBlockTransactionData(it.transaction).getData().SW_UNIQUE_ID
            }

        Log.i("Coin", "${distinctById.size} unique wallets founds. Adding if not present already.")

        for (wallet in distinctById) {
            val currentId = SWJoinBlockTransactionData(wallet.transaction).getData().SW_UNIQUE_ID
            if (!walletIds.contains(currentId)) {
                fetchedWallets.add(wallet)
            }
        }
    }

    /**
     * Load shared wallet trust chain blocks. Blocks are crawled from trust chain users and loaded
     * from the local database.
     */
    private fun updateSharedWalletsUI() {
        lifecycleScope.launchWhenStarted {
            val publicKey = getTrustChainCommunity().myPeer.publicKey.keyToBin().toHex()

            // Update the list view with the found shared wallets
            adapter = SharedWalletListAdapter(
                this@JoinNetworkFragment,
                fetchedWallets,
                publicKey,
                "Click to join",
                disableOnUserJoined = true
            )

            list_view.adapter = adapter
            list_view.setOnItemClickListener { _, view, position, id ->
                lifecycleScope.launch {
                    withContext(Dispatchers.IO) {
                        joinSharedWalletClicked(fetchedWallets[position])
                        Log.i("Coin", "Clicked: $view, $position, $id")
                    }
                }
            }
        }
    }

    /**
     * Crawl all shared wallet blocks of users in the trust chain.
     */
    private suspend fun crawlAvailableSharedWallets() {
        val allUsers = getDemoCommunity().getPeers()
        Log.i("Coin", "Found ${allUsers.size} peers, crawling")

        for (peer in allUsers) {
            try {
                withTimeout(SW_CRAWLING_TIMEOUT_MILLI) {
                    trustchain.crawlChain(peer)
                    val crawlResult = trustchain
                        .getChainByUser(peer.publicKey.keyToBin())

                    updateSharedWallets(crawlResult)
                }
            } catch (t: Throwable) {
                val message = t.message ?: "no message"
                Log.i("Coin", "Crawling failed for: ${peer.publicKey} message: $message")
            }
        }
        disableRefresher()
    }

    /**
     * Join a shared bitcoin wallet.
     */
    fun joinSharedWalletClicked(block: TrustChainBlock) {
        val mostRecentSWBlock =
            getCoinCommunity().fetchLatestSharedWalletBlock(block.calculateHash())
                ?: block

        // Add a proposal to trust chain to join a shared wallet
        val proposeBlockData =
            getCoinCommunity().proposeJoinWallet(
                mostRecentSWBlock.transaction
            ).getData()

        // Wait and collect signatures
        var signatures: List<String>? = null
        while (signatures == null) {
            Thread.sleep(1000)
            signatures = collectJoinWalletSignatures(proposeBlockData)
        }

        // Create a new shared wallet using the signatures of the others.
        // Broadcast the new shared bitcoin wallet on trust chain.
        try {
            getCoinCommunity().joinBitcoinWallet(
                mostRecentSWBlock.transaction,
                proposeBlockData,
                signatures,
                ::updateAlertLabel
            )
        } catch (t: Throwable) {
            Log.i("Coin", "Joining failed: ${t.message ?: '-'}")
            activity?.runOnUiThread {
                alert_tf.text = t.message ?: "Unexpected error occurred. Try again"
            }
        }

        // Update wallets UI list
        fetchSharedWalletsAndUpdateUI()
        activity?.runOnUiThread {
            alert_tf.text = "You joined ${proposeBlockData.SW_UNIQUE_ID}!"
        }
    }

    private fun updateAlertLabel(progress: Double) {
        Log.i("Coin", "Coin: broadcast of create genesis wallet transaction progress: $progress.")

        activity?.runOnUiThread {
            if (progress >= 1) {
                alert_tf?.text = "Join wallet progress: completed!"
            } else {
                val progressString = "%.0f".format(progress * 100)
                alert_tf.text = "Join wallet progress: $progressString%..."
            }
        }
    }

    /**
     * Collect the signatures of a join proposal. Returns true if enough signatures are found.
     */
    private fun collectJoinWalletSignatures(
        blockData: SWSignatureAskBlockTD
    ): List<String>? {
        val signatures =
            getCoinCommunity().fetchProposalSignatures(
                blockData.SW_UNIQUE_ID,
                blockData.SW_UNIQUE_PROPOSAL_ID
            )
        Log.i(
            "Coin",
            "Waiting for signatures. ${signatures.size}/${blockData.SW_SIGNATURES_REQUIRED} received!"
        )

        activity?.runOnUiThread {
            alert_tf?.text =
                "Collecting signatures: ${signatures.size}/${blockData.SW_SIGNATURES_REQUIRED} received!"
        }

        if (signatures.size >= blockData.SW_SIGNATURES_REQUIRED) {
            return signatures
        }
        return null
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_join_network, container, false)
    }

    companion object {
        @JvmStatic
        fun newInstance() = JoinNetworkFragment()

        public const val SW_CRAWLING_TIMEOUT_MILLI: Long = 1_000 * 10
    }
}
