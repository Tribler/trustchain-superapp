package nl.tudelft.trustchain.detoks

import android.app.AlertDialog
import android.os.Bundle
import android.view.View
import android.widget.Button
import androidx.navigation.Navigation
import nl.tudelft.ipv8.IPv8
import nl.tudelft.ipv8.Peer
import nl.tudelft.ipv8.android.IPv8Android
import nl.tudelft.ipv8.attestation.trustchain.BlockListener
import nl.tudelft.ipv8.attestation.trustchain.TrustChainBlock
import nl.tudelft.ipv8.attestation.trustchain.TrustChainCommunity
import nl.tudelft.ipv8.util.toHex
import nl.tudelft.trustchain.common.ui.BaseFragment
import nl.tudelft.trustchain.common.util.viewBinding
import nl.tudelft.trustchain.detoks.databinding.FragmentTestBinding

class TestFragment : BaseFragment(R.layout.fragment_test) {

    private val binding by viewBinding(FragmentTestBinding::bind)

    private lateinit var ipv8: IPv8
    private lateinit var community: DeToksCommunity
    private lateinit var trustchainCommunity: TrustChainCommunity

    private val BLOCK_TYPE = "our_test_block"

    private var index = 0

    // the peer to send to
    private lateinit var targetPeer: Peer

    private fun debugLog(txt: String) {
        val textView = binding.debugTextView
        textView.text = textView.text.toString() + "${txt}\n"
    }

    private fun createProposal(recipient: Peer) {
        val transaction = mapOf("proposal" to index)
        debugLog(
            "Proposing block: ${transaction["proposal"]} to ${
                recipient.key.keyToBin().toHex().take(10)
            }..."
        )
        trustchainCommunity.createProposalBlock(
            BLOCK_TYPE,
            transaction,
            recipient.publicKey.keyToBin()
        )
        index += 1
    }

    private fun createAgreement(recipient: Peer, block: TrustChainBlock) {
        val transaction = mapOf("agreement" to block.transaction["proposal"])
        debugLog(
            "Agreeing block: ${transaction["agreement"]} to ${
                recipient.key.keyToBin().toHex().take(10)
            }..."
        )
        trustchainCommunity.createAgreementBlock(
            block,
            transaction,
        )
    }

    private fun setTargetPeer(tpeer: Peer) {
        this.targetPeer = tpeer
        binding.targetPeerTextView.text = "target peer:" + tpeer.publicKey.toString()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {

        // get references to community etc
        community = IPv8Android.getInstance().getOverlay()!!
        trustchainCommunity = IPv8Android.getInstance().getOverlay()!!
        ipv8 = IPv8Android.getInstance()


        // initial target peer is yourself
        setTargetPeer(community.myPeer)

        // display your PK
        binding.yourPkTextView.text = "Your PK: ${community.myPeer.publicKey}"

        // button for finding peers
        binding.findPeersButton.setOnClickListener {
            val peers = community.getPeers()
            val peersList = peers.map { it.publicKey.toString() }.joinToString("\n")

            if (peers.isEmpty()) {
                binding.peersList.text = "no peers found"
            } else {
                binding.peersList.text = peersList
                setTargetPeer(peers[0])
            }

        }

        // button1 for creating a proposal
        binding.button1.setOnClickListener {
            setTargetPeer(targetPeer)
            createProposal(targetPeer)
        }

        binding.debugClearButton.setOnClickListener {
            binding.debugTextView.text = ""
            binding.debugTextView.maxLines = Integer.MAX_VALUE
            binding.debugTextView.maxWidth = Integer.MAX_VALUE
        }
        val environmentSwitchButton = view.findViewById<Button>(R.id.toTransacFreq)
        environmentSwitchButton.setOnClickListener { switchEnvirmonments(view) }


        trustchainCommunity.addListener(BLOCK_TYPE, object : BlockListener {
            override fun onBlockReceived(block: TrustChainBlock) {

                if (!block.publicKey.contentEquals(ipv8.myPeer.publicKey.keyToBin())) {

                    if (block.isProposal) {
                        val builder = AlertDialog.Builder(requireContext())
                        builder.setTitle("Proposal Block received!")
                        builder.setMessage("Sender ${block.publicKey.toHex().take(10)}. Do you agree?")
                        builder.setPositiveButton("Yes") { _, _ ->
                            // TODO make sender
                            createAgreement(targetPeer, block)
                        }
                        val dialog = builder.create()
                        dialog.show()
                    } else if (block.isAgreement) {
                        val builder = AlertDialog.Builder(requireContext())
                        builder.setTitle("Agreement Block received!")
                        builder.setMessage("Sender ${block.publicKey.toHex().take(10)}")
                        builder.setNeutralButton("Nice") { _, _ ->
                        }
                        val dialog = builder.create()
                        dialog.show()
                    }
                }
            }
        })


    }
    fun switchEnvirmonments(view: View){
        val navController = Navigation.findNavController(view)
        navController.navigate(R.id.action_go_to_testFreq)
    }
}
