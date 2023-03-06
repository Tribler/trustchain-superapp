package nl.tudelft.trustchain.detoks

import android.os.Bundle
import android.view.View
import nl.tudelft.ipv8.Community
import nl.tudelft.ipv8.IPv8
import nl.tudelft.ipv8.android.IPv8Android
import nl.tudelft.ipv8.attestation.trustchain.TrustChainCommunity
import nl.tudelft.ipv8.keyvault.PublicKey
import nl.tudelft.trustchain.common.DemoCommunity
import nl.tudelft.trustchain.common.ui.BaseFragment
import nl.tudelft.trustchain.common.util.viewBinding
import nl.tudelft.trustchain.detoks.databinding.FragmentExampleoverlayBinding

class ExampleOverlayFragment : BaseFragment(R.layout.fragment_exampleoverlay) {

    private val binding by viewBinding(FragmentExampleoverlayBinding::bind)
    private lateinit var ipv8: IPv8
    private lateinit var community: Community

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {

        community = IPv8Android.getInstance().getOverlay<DemoCommunity>()!!
        val peers = community.getPeers()



//        // Example of how to change the page
//        super.onViewCreated(view, savedInstanceState)
//
        binding.button1.setOnClickListener {
            for (peer in peers) {
                sendToken(
                    1,
                    community.myPeer.publicKey,
                    peer?.publicKey ?: community.myPeer.publicKey
                )
                binding.textView1.text = "Sent token"

            }
        }

    }

    fun sendToken(value: Int, sender: PublicKey, receiver: PublicKey) {

    }
}
