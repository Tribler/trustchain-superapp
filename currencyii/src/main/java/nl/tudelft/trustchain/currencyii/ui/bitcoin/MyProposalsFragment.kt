package nl.tudelft.trustchain.currencyii.ui.bitcoin

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import nl.tudelft.ipv8.attestation.trustchain.TrustChainBlock
import nl.tudelft.ipv8.util.toHex
import nl.tudelft.trustchain.currencyii.CoinCommunity
import nl.tudelft.trustchain.currencyii.R
import nl.tudelft.trustchain.currencyii.databinding.FragmentMyProposalsBinding
import nl.tudelft.trustchain.currencyii.sharedWallet.SWJoinBlockTransactionData
import nl.tudelft.trustchain.currencyii.sharedWallet.SWSignatureAskTransactionData
import nl.tudelft.trustchain.currencyii.sharedWallet.SWTransferFundsAskTransactionData
import nl.tudelft.trustchain.currencyii.ui.BaseFragment

/**
 * A simple [Fragment] subclass.
 * Use the [MyProposalsFragment] factory method to
 * create an instance of this fragment.
 */
class MyProposalsFragment : BaseFragment(R.layout.fragment_my_proposals) {
    private var _binding: FragmentMyProposalsBinding? = null
    private val binding get() = _binding!!

    private var proposals: ArrayList<TrustChainBlock> = ArrayList()

    /**
     * Get all proposals for the user and show them in the UI
     */
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

    /**
     * Update the proposals and show them in the UI
     */
    private fun updateProposalListUI() {
        activity?.runOnUiThread {
            val uniqueProposals: ArrayList<TrustChainBlock> = ArrayList()
            val proposalCopy = arrayListOf<TrustChainBlock>()
            proposalCopy.addAll(proposals)

            for (proposal in proposalCopy) {
                if (!uniqueProposals.contains(proposal) && isUserInWallet(proposal)) uniqueProposals.add(
                    proposal
                )
            }
            val adaptor = ProposalListAdapter(this, uniqueProposals)
            binding.proposalListView.adapter = adaptor
            binding.proposalListView.setOnItemClickListener { _, _, position, _ ->
                val block = uniqueProposals[position]
                if (block.type == CoinCommunity.TRANSFER_FUNDS_ASK_BLOCK) {
                    try {
                        val bundle = bundleOf("type" to block.type, "blockId" to block.blockId)
                        findNavController().navigate(R.id.votesFragment, bundle)
                    } catch (t: Throwable) {
                        Log.e("Coin", "transfer voting failed: ${t.message ?: "no message"}")
                    }
                }
                if (block.type == CoinCommunity.SIGNATURE_ASK_BLOCK) {
                    try {
                        val bundle = bundleOf("type" to block.type, "blockId" to block.blockId)
                        findNavController().navigate(R.id.votesFragment, bundle)
                    } catch (t: Throwable) {
                        Log.e("Coin", "join voting failed: ${t.message ?: "no message"}")
                    }
                }
            }
        }
    }

    /**
     * Check whether the user is in that wallet, otherwise he should not see that specific proposal
     * @param proposal - the concerning wallet
     * @return Boolean - if the user is in the wallet
     */
    private fun isUserInWallet(proposal: TrustChainBlock): Boolean {
        val walletID = if (proposal.type == CoinCommunity.SIGNATURE_ASK_BLOCK) {
            SWSignatureAskTransactionData(proposal.transaction).getData().SW_UNIQUE_ID
        } else {
            SWTransferFundsAskTransactionData(proposal.transaction).getData().SW_UNIQUE_ID
        }
        return getUserWalletIds().contains(walletID)
    }

    /**
     * Get all the wallets of the user
     * @return list of wallet ids
     */
    private fun getUserWalletIds(): List<String> {
        val myPublicKey = getTrustChainCommunity().myPeer.publicKey.keyToBin().toHex()
        val wallets = getCoinCommunity().fetchLatestJoinedSharedWalletBlocks()
            .map { SWJoinBlockTransactionData(it.transaction).getData() }
        val userWallets = wallets.filter { it.SW_TRUSTCHAIN_PKS.contains(myPublicKey) }
        return userWallets.map { it.SW_UNIQUE_ID }
    }

    /**
     * Update the currently stored proposals. Only new and unique proposals are added.
     * @param newProposals - the new proposals that need to be added to the UI
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
                // TODO: Commented this line out, it causes the app to crash
//                withTimeout(JoinDAOFragment.SW_CRAWLING_TIMEOUT_MILLI) {
                trustchain.crawlChain(peer)
                val crawlResult = trustchain
                    .getChainByUser(peer.publicKey.keyToBin())
                    .filter {
                        (
                            it.type == CoinCommunity.SIGNATURE_ASK_BLOCK ||
                                it.type == CoinCommunity.TRANSFER_FUNDS_ASK_BLOCK
                            ) && !getCoinCommunity().checkEnoughFavorSignatures(it)
                    }
                Log.i(
                    "Coin",
                    "Crawl result: ${crawlResult.size} proposals found (from ${peer.address})"
                )
                if (crawlResult.isNotEmpty()) {
                    updateProposals(crawlResult)
                    updateProposalListUI()
                }
//                }
            } catch (t: Throwable) {
                val message = t.message ?: "no message"
                Log.e("Coin", "Crawling failed for: ${peer.address} message: $message")
            }
        }
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
    ): View {
        // Inflate the layout for this fragment
        _binding = FragmentMyProposalsBinding.inflate(inflater, container, false)
        val view = binding.root
        return view
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
