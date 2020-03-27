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
<<<<<<< Updated upstream
=======
import nl.tudelft.ipv8.android.demo.sharedWallet.SWSignatureAskTransactionData
import nl.tudelft.ipv8.android.demo.sharedWallet.SWUtil
>>>>>>> Stashed changes
import nl.tudelft.ipv8.android.demo.ui.BaseFragment
import nl.tudelft.ipv8.android.demo.ui.users.UserItem
import nl.tudelft.ipv8.android.keyvault.AndroidCryptoProvider
import nl.tudelft.ipv8.attestation.trustchain.TrustChainBlock
import nl.tudelft.ipv8.util.toHex

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

//        var foundWallets = waitForCrawlAvailableSharedWallets().filter {
//            CoinCommunity.SW_TRANSACTION_BLOCK_KEYS.contains(it.type)
//        }.distinctBy {
//            SWUtil.parseTransaction(it.transaction).getString(CoinCommunity.SW_UNIQUE_ID)
//        }

//        Log.i("Coin", foundWallets.joinToString())

//        val sharedWalletBlocks = getCoinCommunity().discoverSharedWallets()
        loadSharedWallets()
    }

    private fun loadSharedWallets() {
        lifecycleScope.launchWhenStarted {
            var foundWallets = waitForCrawlAvailableSharedWallets().filter {
                CoinCommunity.SW_TRANSACTION_BLOCK_KEYS.contains(it.type)
            }.distinctBy {
                SWUtil.parseTransaction(it.transaction).getString(CoinCommunity.SW_UNIQUE_ID)
            }
            Log.i("Coin", "${foundWallets.size} peers unique shared wallets found")

            val publicKey = getTrustChainCommunity().myPeer.publicKey.keyToBin().toHex()

            adapter = SharedWalletListAdapter(
                this@JoinNetworkFragment,
                foundWallets,
                publicKey,
                "Click to join"
            )
            list_view.adapter = adapter
            list_view.setOnItemClickListener { _, view, position, id ->
                joinSharedWalletClicked(foundWallets[position])
                Log.i("Coin", "Clicked: $view, $position, $id")
            }
        }
    }

    private fun waitForCrawlAvailableSharedWallets(): ArrayList<TrustChainBlock> {
        var foundWallets: ArrayList<TrustChainBlock> = arrayListOf()
        runBlocking {
            //            withTimeout(TrustChainCrawler.CHAIN_CRAWL_TIMEOUT) {
            foundWallets = crawlAvailableSharedWallets()
//            }
        }
        return foundWallets
    }

    private suspend fun crawlAvailableSharedWallets(): ArrayList<TrustChainBlock> {
        val allPeers = getTrustChainCommunity().getPeers()
        Log.i("Coin", "${allPeers.size} peers found")
        val discoveredBlocks: ArrayList<TrustChainBlock> = arrayListOf()

        for (peer in allPeers) {
            discoveredBlocks.addAll(
                getTrustChainCommunity().sendCrawlRequest(
                    peer,
                    peer.publicKey.keyToBin(),
                    LongRange(-1, -1)
                )
            )
        }

        return discoveredBlocks
    }

    private fun joinSharedWalletClicked(block: TrustChainBlock) {
        val transactionId = getCoinCommunity().joinSharedWallet(block.calculateHash())
        fetchCurrentSharedWalletStatusLoop(transactionId) // TODO: cleaner solution for blocking
        getCoinCommunity().addSharedWalletJoinBlock(block.calculateHash())
    }

<<<<<<< Updated upstream
=======
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

>>>>>>> Stashed changes
    private fun fetchCurrentSharedWalletStatusLoop(transactionId: String) {
        var finished = false

        while (!finished) {
            finished = getCoinCommunity().fetchJoinSharedWalletStatus(transactionId)
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
    }
}
