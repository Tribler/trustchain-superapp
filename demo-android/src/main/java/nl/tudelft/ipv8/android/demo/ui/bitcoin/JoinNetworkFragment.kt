package nl.tudelft.ipv8.android.demo.ui.bitcoin

import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import kotlinx.android.synthetic.main.fragment_join_network.*
import nl.tudelft.ipv8.android.demo.R
import nl.tudelft.ipv8.android.demo.sharedWallet.SWSignatureAskTransactionData
import nl.tudelft.ipv8.android.demo.ui.BaseFragment
import nl.tudelft.ipv8.attestation.trustchain.TrustChainBlock
import nl.tudelft.ipv8.util.toHex
import kotlin.concurrent.thread

/**
 * A simple [Fragment] subclass.
 * Use the [BitcoinFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class JoinNetworkFragment(
    override val controller: BitcoinViewController
) : BitcoinView, BaseFragment(R.layout.fragment_join_network) {
    private val tempBitcoinPk = ByteArray(2)

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        val sharedWalletBlocks = getCoinCommunity().discoverSharedWallets()
        val publicKey = getTrustChainCommunity().myPeer.publicKey.keyToBin().toHex()
        val adaptor = SharedWalletListAdapter(this, sharedWalletBlocks, publicKey, "Click to join")
        list_view.adapter = adaptor
        list_view.setOnItemClickListener { _, view, position, id ->
            joinSharedWalletClicked(sharedWalletBlocks[position])
            Log.i("Coin", "Clicked: $view, $position, $id")
        }
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
            getCoinCommunity().fetchJoinSignatures(blockData.SW_UNIQUE_ID, blockData.SW_UNIQUE_PROPOSAL_ID)

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
        fun newInstance(controller: BitcoinViewController) = JoinNetworkFragment(controller)
    }
}
