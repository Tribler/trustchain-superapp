package nl.tudelft.trustchain.currencyii.ui.bitcoin

import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.lifecycleScope
import kotlinx.android.synthetic.main.fragment_join_network.*
import kotlinx.coroutines.*
import nl.tudelft.trustchain.currencyii.CoinCommunity
import nl.tudelft.trustchain.currencyii.sharedWallet.SWUtil
import nl.tudelft.trustchain.currencyii.ui.BaseFragment
import nl.tudelft.ipv8.attestation.trustchain.TrustChainBlock
import nl.tudelft.ipv8.util.toHex
import nl.tudelft.trustchain.currencyii.R
import nl.tudelft.trustchain.currencyii.sharedWallet.SWSignatureAskBlockTD

/**
 * A simple [Fragment] subclass.
 * Use the [BitcoinFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class JoinNetworkFragment() : BaseFragment(R.layout.fragment_join_network) {
    private var adapter: SharedWalletListAdapter? = null

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        loadSharedWallets()
    }

    /**
     * Load shared wallet trust chain blocks. Blocks are crawled from trust chain users and loaded
     * from the local database.
     */
    private fun loadSharedWallets() {
        lifecycleScope.launchWhenStarted {
            val discoveredWallets = getCoinCommunity().discoverSharedWallets()
            val foundWallets = withContext(Dispatchers.IO) {
                crawlAvailableSharedWallets()
            }

            Log.i(
                "Coin",
                "${foundWallets.size} found with crawling and ${discoveredWallets.size} in database"
            )

            // Filter the wallets on the correct block type and unique wallet ids
            val allWallets = discoveredWallets
                .union(foundWallets)
                .filter {
                    CoinCommunity.SW_TRANSACTION_BLOCK_KEYS.contains(it.type)
                }.distinctBy {
                    SWUtil.parseTransaction(it.transaction).get(CoinCommunity.SW_UNIQUE_ID).asString
                }

            val publicKey = getTrustChainCommunity().myPeer.publicKey.keyToBin().toHex()

            // Update the list view with the found shared wallets
            adapter = SharedWalletListAdapter(
                this@JoinNetworkFragment,
                allWallets,
                publicKey,
                "Click to join"
            )
            list_view.adapter = adapter
            list_view.setOnItemClickListener { _, view, position, id ->
                joinSharedWalletClicked(allWallets[position])
                Log.i("Coin", "Clicked: $view, $position, $id")
            }
        }
    }

    /**
     * Crawl all shared wallet blocks of users in the trust chain.
     */
    private suspend fun crawlAvailableSharedWallets(): ArrayList<TrustChainBlock> {
        val allUsers = getDemoCommunity().getPeers()
        Log.i("Coin", "Found ${allUsers.size} peers, crawling")
        val discoveredBlocks: ArrayList<TrustChainBlock> = arrayListOf()

        for (peer in allUsers) {
            try {
                withTimeout(SW_CRAWLING_TIMEOUT_MILLI) {
                    trustchain.crawlChain(peer)
                    var crawlResult = trustchain
                        .getChainByUser(peer.publicKey.keyToBin())
                        .filter {
                            CoinCommunity.SW_TRANSACTION_BLOCK_KEYS.contains(it.type)
                        }.distinctBy {
                            SWUtil.parseTransaction(it.transaction).get(CoinCommunity.SW_UNIQUE_ID)
                                .asString
                        }
                    discoveredBlocks.addAll(crawlResult)
                }
            } catch (t: Throwable) {
                val message = t.message ?: "no message"
                Log.i("Coin", "Crawling failed for: ${peer.publicKey} message: $message")
            }
        }

        return discoveredBlocks
    }

    /**
     * Join a shared bitcoin wallet.
     */
    private fun joinSharedWalletClicked(block: TrustChainBlock) {
        // Add a proposal to trust chain to join a shared wallet
        val proposeBlockData =
            getCoinCommunity().proposeJoinWalletOnTrustChain(
                block.transaction
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
            getCoinCommunity().safeSendingJoinWalletTransaction(
                block.transaction,
                proposeBlockData,
                signatures,
                ::updateAlertLabel
            )
        } catch (t: Throwable) {
            Log.i("Coin", "Joining failed: ${t.message ?: '-'}")
        }

        // Update wallets UI list
        loadSharedWallets()
    }

    private fun updateAlertLabel(progress: Double) {
        Log.i("Coin", "Coin: broadcast of join transaction progress: $progress.")
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
            "Waiting for signatures. ${signatures.size}/${blockData.SW_SIGNATURES_REQUIRED} found!"
        )
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

        public const val SW_CRAWLING_TIMEOUT_MILLI: Long = 5_000
    }
}
