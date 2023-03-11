package nl.tudelft.trustchain.detoks

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
import nl.tudelft.ipv8.attestation.trustchain.TrustChainCommunity
import nl.tudelft.ipv8.attestation.trustchain.TrustChainTransaction
import nl.tudelft.trustchain.common.ui.BaseFragment
import nl.tudelft.trustchain.common.util.viewBinding
import nl.tudelft.trustchain.detoks.databinding.FragmentExampleoverlayBinding

class ExampleOverlayFragment : BaseFragment(R.layout.fragment_exampleoverlay) {

    private val binding by viewBinding(FragmentExampleoverlayBinding::bind)

    private lateinit var ipv8: IPv8
    private lateinit var community: OurCommunity
    private lateinit var trustchainCommunity : TrustChainCommunity

    private fun createProposal(recipient : Peer) {
        val transaction = mapOf("msg" to "test_message")
        trustchainCommunity.createProposalBlock("test_dlock", transaction, recipient.publicKey.keyToBin())
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {

        community = IPv8Android.getInstance().getOverlay()!!
        trustchainCommunity = IPv8Android.getInstance().getOverlay()!!
        ipv8 = IPv8Android.getInstance()


        binding.peer1IpTextview.text = "${ipv8.myPeer.address}"

//        // busy wait for peers
//        var peers = community.getPeers()
//        while (peers.isEmpty()) {
//            peers = community.getPeers()
//        }
//
//        binding.peer2IpTextview.text = "${peers[0].address}"


        binding.peer1SendTokenButton.setOnClickListener {

            Log.d("OurCommunity", "Broadcasting")

            lifecycleScope.launch {
                while (isActive) {
                    community.broadcastGreeting()
                    delay(1000)
                }
            }
        }


    }
}
