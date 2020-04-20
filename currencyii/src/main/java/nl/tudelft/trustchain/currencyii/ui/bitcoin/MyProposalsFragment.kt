package nl.tudelft.trustchain.currencyii.ui.bitcoin

import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.lifecycleScope
import kotlinx.android.synthetic.main.fragment_my_proposals.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import nl.tudelft.ipv8.attestation.trustchain.TrustChainBlock
import nl.tudelft.trustchain.currencyii.CoinCommunity
import nl.tudelft.trustchain.currencyii.R
import nl.tudelft.trustchain.currencyii.sharedWallet.SWSignatureAskTransactionData
import nl.tudelft.trustchain.currencyii.sharedWallet.SWTransferFundsAskTransactionData
import nl.tudelft.trustchain.currencyii.ui.BaseFragment

/**
 * A simple [Fragment] subclass.
 * Use the [MyProposalsFragment] factory method to
 * create an instance of this fragment.
 */
class MyProposalsFragment : BaseFragment(R.layout.fragment_my_proposals) {
    private var proposals: ArrayList<TrustChainBlock> = ArrayList()

    private fun fetchProposalsAndUpdateUI() {
        lifecycleScope.launch {
            withContext(Dispatchers.IO) {
                val databaseProposals = getCoinCommunity().fetchProposalBlocks()
                Log.i("Coin", "${databaseProposals.size} proposals found in database!")
                updateProposals(databaseProposals)
                updateProposalListUI()
                crawlProposalsAndUpdateIfNewFound()
            }
        }
    }

    private fun updateProposalListUI() {
        activity?.runOnUiThread {
            val adaptor = ProposalListAdapter(this, proposals)
            proposal_list_view.adapter = adaptor
            proposal_list_view.setOnItemClickListener { _, _, position, _ ->
                val block = proposals[position]
                if (block.type == CoinCommunity.TRANSFER_FUNDS_ASK_BLOCK) {
                    Log.i("Coin", "Voted yes on transferring funds of: ${block.transaction}")
                    CoinCommunity.transferFundsBlockReceived(block, getTrustChainCommunity().myPeer.publicKey.keyToBin())
                }
                if (block.type == CoinCommunity.SIGNATURE_ASK_BLOCK) {
                    Log.i("Coin", "Voted yes on joining of: ${block.transaction}")
                    CoinCommunity.joinAskBlockReceived(block, getTrustChainCommunity().myPeer.publicKey.keyToBin())
                }
            }
        }
    }

    /**
     * Update the currently stored proposals. Only new and unique proposals are added.
     */
    private fun updateProposals(newProposals: List<TrustChainBlock>) {
        val coinCommunity = getCoinCommunity()
        val proposalIds = proposals.map {
            coinCommunity.fetchSignatureRequestProposalId(it)
        }
        val distinctById = newProposals.distinctBy {
            coinCommunity.fetchSignatureRequestProposalId(it)
        }

        for (proposal in distinctById) {
            val currentId = coinCommunity.fetchSignatureRequestProposalId(proposal)
            if (!proposalIds.contains(currentId)) {
                proposals.add(proposal)
            }
        }
    }

    /**
     * Crawl all shared wallet blocks of users in the trust chain.
     */
    private suspend fun crawlProposalsAndUpdateIfNewFound() {
        val allUsers = getDemoCommunity().getPeers()
        Log.i("Coin", "Found ${allUsers.size} peers, crawling")

        for (peer in allUsers) {
            try {
                withTimeout(JoinNetworkFragment.SW_CRAWLING_TIMEOUT_MILLI) {
                    trustchain.crawlChain(peer)
                    val crawlResult = trustchain
                        .getChainByUser(peer.publicKey.keyToBin())
                        .filter {
                            it.type == CoinCommunity.SIGNATURE_ASK_BLOCK ||
                                it.type == CoinCommunity.TRANSFER_FUNDS_ASK_BLOCK
                        }
                    Log.i("Coin", "Crawl result: ${crawlResult.size} proposals found (from ${peer.address})")
                    if (crawlResult.isNotEmpty()) {
                        updateProposals(crawlResult)
                        updateProposalListUI()
                    }
                }
            } catch (t: Throwable) {
                val message = t.message ?: "no message"
                Log.i("Coin", "Crawling failed for: ${peer.address} message: $message")
            }
        }
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        lifecycleScope.launchWhenStarted {
            fetchProposalsAndUpdateUI()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_my_proposals, container, false)
    }
}
