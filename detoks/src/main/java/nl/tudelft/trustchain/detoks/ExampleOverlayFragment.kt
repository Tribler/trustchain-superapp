package nl.tudelft.trustchain.detoks

import android.os.Bundle
import android.view.View
import nl.tudelft.ipv8.Community
import nl.tudelft.ipv8.IPv8
import nl.tudelft.ipv8.android.IPv8Android
import nl.tudelft.ipv8.attestation.trustchain.TrustChainCommunity
import nl.tudelft.trustchain.common.DemoCommunity
import nl.tudelft.trustchain.common.ui.BaseFragment
import nl.tudelft.trustchain.common.util.viewBinding
import nl.tudelft.trustchain.detoks.databinding.FragmentExampleoverlayBinding

class ExampleOverlayFragment : BaseFragment(R.layout.fragment_exampleoverlay) {

    private val binding by viewBinding(FragmentExampleoverlayBinding::bind)
    private lateinit var ipv8: IPv8
    private lateinit var community: Community

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {

        community = IPv8Android.getInstance().getOverlay<OurCommunity>()!!

       // Example of how to change the page
        binding.button1.setOnClickListener {

            binding.textView1.text = community.serviceId;

            binding.textView1.setText("peers:\n")

            val peers = community.getPeers()

            for (peer in peers) {
                binding.textView1.append("${peer.address}, ${peer.key}\n")
            }

        }


    }
}
