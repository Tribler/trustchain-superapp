package nl.tudelft.trustchain.detoks

import android.app.AlertDialog
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import nl.tudelft.ipv8.IPv8
import nl.tudelft.ipv8.Peer
import nl.tudelft.ipv8.android.IPv8Android
import nl.tudelft.ipv8.attestation.trustchain.BlockListener
import nl.tudelft.ipv8.attestation.trustchain.TrustChainBlock
import nl.tudelft.ipv8.attestation.trustchain.TrustChainCommunity
import nl.tudelft.ipv8.attestation.trustchain.TrustChainTransaction
import nl.tudelft.ipv8.util.toHex
import nl.tudelft.trustchain.common.ui.BaseFragment
import nl.tudelft.trustchain.common.util.viewBinding
import nl.tudelft.trustchain.detoks.databinding.FragmentExampleoverlayBinding

class ExampleOverlayFragment : BaseFragment(R.layout.fragment_exampleoverlay) {

    private val binding by viewBinding(FragmentExampleoverlayBinding::bind)

    private lateinit var ipv8: IPv8
    private lateinit var community: OurCommunity
    private lateinit var trustchainCommunity : TrustChainCommunity
    private val BLOCK_TYPE = "our_test_block"

    private var transaction_index = 0;

    private fun createProposal(recipient: Peer, peer_id: Int) {
        val transaction = mapOf("proposal" to transaction_index, "peer_id" to peer_id)
        trustchainCommunity.createProposalBlock(
            BLOCK_TYPE,
            transaction,
            recipient.publicKey.keyToBin()
        )
        transaction_index += 1
    }

    private fun createAgreement(block: TrustChainBlock) {
        val transaction = mapOf("agreement" to block.transaction["proposal"], "peer_id" to block.transaction["peer_id"])
        trustchainCommunity.createAgreementBlock(
            block,
            transaction,
        )
    }
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {

        community = IPv8Android.getInstance().getOverlay()!!
        trustchainCommunity = IPv8Android.getInstance().getOverlay()!!
        ipv8 = IPv8Android.getInstance()


        binding.peer1IpTextview.text = "${ipv8.myPeer.address.ip}"
        binding.peer2IpTextview.text = "${ipv8.myPeer.address.ip}"

        binding.peer1SendTokenButton.setOnClickListener {
            binding.peer1TokensTextview.text="Transaction ${transaction_index}"
            createProposal(ipv8.myPeer, 1)
        }
        binding.peer2SendTokenButton.setOnClickListener {
            binding.peer2TokensTextview.text="Transaction ${transaction_index}"
            createProposal(ipv8.myPeer, 2)
        }

        trustchainCommunity.addListener(BLOCK_TYPE, object : BlockListener {
            override fun onBlockReceived(block: TrustChainBlock) {
                if (block.isProposal){
                    val builder = AlertDialog.Builder(requireContext())
                    builder.setTitle("Proposal Block received from peer ${block.transaction["peer_id"]}!")
                    builder.setMessage("Transaction ${block.transaction["proposal"]}. Do you want to confirm it?")
                    builder.setPositiveButton("Yes") { _, _ ->
                        if(block.transaction["peer_id"] == 1){
                            binding.peer1ConfirmTextview.text="Transaction ${block.transaction["proposal"]}"
                        } else if (block.transaction["peer_id"] == 2) {
                            binding.peer2ConfirmTextview.text="Transaction ${block.transaction["proposal"]}"
                        }
                        createAgreement(block)
                    }
                    val dialog= builder.create()
                    dialog.show()
                }else if (block.isAgreement){
                    if(block.transaction["peer_id"] == 1){
                        val builder = AlertDialog.Builder(requireContext())
                        builder.setTitle("Peer 2 has agreed your proposal!")
                        builder.setNeutralButton("OK"){ _ , _ ->}
                        val dialog= builder.create()
                        dialog.show()
                    } else {
                        val builder = AlertDialog.Builder(requireContext())
                        builder.setTitle("Peer 1 has agreed your proposal!")
                        builder.setNeutralButton("OK"){ _ , _ ->}
                        val dialog= builder.create()
                        dialog.show()
                    }
                }
            }
        })
    }
}
