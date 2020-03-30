package nl.tudelft.ipv8.android.demo.ui.bitcoin

import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.lifecycleScope
import kotlinx.android.synthetic.main.fragment_join_network.*
import kotlinx.coroutines.*
import nl.tudelft.ipv8.android.demo.CoinCommunity
import nl.tudelft.ipv8.android.demo.R
import nl.tudelft.ipv8.android.demo.sharedWallet.SWSignatureAskTransactionData
import nl.tudelft.ipv8.android.demo.sharedWallet.SWUtil
import nl.tudelft.ipv8.android.demo.ui.BaseFragment
import nl.tudelft.ipv8.attestation.trustchain.TrustChainBlock
import nl.tudelft.ipv8.attestation.trustchain.TrustChainCrawler
import nl.tudelft.ipv8.util.toHex
import kotlin.concurrent.thread

/**
 * A simple [Fragment] subclass.
 * Use the [BitcoinFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class JoinNetworkFragment(
) : BaseFragment(R.layout.fragment_join_network) {
    private val tempBitcoinPk = ByteArray(2)
    private var adapter: SharedWalletListAdapter? = null

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        loadSharedWallets()
    }

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

    private suspend fun crawlAvailableSharedWallets(): ArrayList<TrustChainBlock> {
        val allUsers = trustchain.getUsers()
        val discoveredBlocks: ArrayList<TrustChainBlock> = arrayListOf()

        for (index in allUsers.indices) {
            val publicKey = allUsers[index].publicKey
            // Continue if the peer is not found!
            val peer = trustchain.getPeerByPublicKeyBin(publicKey) ?: continue
            try {
                withTimeout(SW_CRAWLING_TIMEOUT_MILLI) {
                    trustchain.crawlChain(peer)
                    var crawlResult = trustchain
                        .getChainByUser(peer.publicKey.keyToBin())

                    crawlResult = crawlResult.filter {
                        CoinCommunity.SW_TRANSACTION_BLOCK_KEYS.contains(it.type)
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

    private fun joinSharedWalletClicked(block: TrustChainBlock) {
        val transactionPackage = getCoinCommunity().createBitcoinSharedWallet(block.calculateHash())
        val proposeBlock =
            getCoinCommunity().proposeJoinWalletOnTrustChain(
                block.calculateHash(),
                transactionPackage.serializedTransaction
            )

        // Wait until the new shared wallet is created
        fetchCurrentSharedWalletStatusLoop(transactionPackage.transactionId) // TODO: cleaner solution for blocking

        // Now start a thread to collect and wait (non-blocking) for signatures
        val requiredSignatures = proposeBlock.getData().SW_SIGNATURES_REQUIRED

        thread(start = true) {
            var finished = false
            while (!finished) {
                finished = collectJoinWalletSignatures(proposeBlock, requiredSignatures)
                Thread.sleep(100)
            }
        }

        getCoinCommunity().addSharedWalletJoinBlock(block.calculateHash())
    }

    /**
     * Collect the signatures of a join proposal. Returns true if enough signatures are found.
     */
    private fun collectJoinWalletSignatures(
        data: SWSignatureAskTransactionData,
        requiredSignatures: Int
    ): Boolean {
        val blockData = data.getData()
        val signatures =
            getCoinCommunity().fetchJoinSignatures(
                blockData.SW_UNIQUE_ID,
                blockData.SW_UNIQUE_PROPOSAL_ID
            )

        if (signatures.size >= requiredSignatures) {
            getCoinCommunity().safeSendingJoinWalletTransaction(data, signatures)
            return true
        }
        return false
    }

    private fun fetchCurrentSharedWalletStatusLoop(transactionId: String) {
        var finished = false

        while (!finished) {
            finished = getCoinCommunity().fetchBitcoinTransactionStatus(transactionId)
            Thread.sleep(1_000)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
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
