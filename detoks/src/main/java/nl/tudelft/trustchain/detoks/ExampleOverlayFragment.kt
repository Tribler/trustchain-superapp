package nl.tudelft.trustchain.detoks

import android.os.Bundle
import android.util.Log
import android.view.View
import nl.tudelft.ipv8.android.IPv8Android
import nl.tudelft.ipv8.attestation.trustchain.TrustChainCommunity
import nl.tudelft.trustchain.common.ui.BaseFragment
import nl.tudelft.trustchain.common.util.viewBinding
import nl.tudelft.trustchain.detoks.databinding.FragmentExampleoverlayBinding

class ExampleOverlayFragment : BaseFragment(R.layout.fragment_exampleoverlay) {

    private val binding by viewBinding(FragmentExampleoverlayBinding::bind)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {

        val community = IPv8Android.getInstance().getOverlay<TrustChainCommunity>()!!
        val peers = community.getPeers()

        for (peer in peers) {
            Log.d("DemoApplication", peer.mid)
        }


//        // Example of how to change the page
//        super.onViewCreated(view, savedInstanceState)
//
//        binding.button1.setOnClickListener {
//            binding.textView1.setText("lmao")
//        }

    }

}
