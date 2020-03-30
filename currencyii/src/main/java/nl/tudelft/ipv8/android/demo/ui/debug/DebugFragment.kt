package nl.tudelft.ipv8.android.demo.ui.debug

import android.os.Bundle
import android.view.View
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.*
import nl.tudelft.ipv8.Community
import nl.tudelft.ipv8.android.demo.R
import nl.tudelft.ipv8.android.demo.databinding.FragmentDebugBinding
import nl.tudelft.ipv8.android.demo.ui.BaseFragment
import nl.tudelft.ipv8.android.demo.util.viewBinding
import nl.tudelft.ipv8.util.toHex

class DebugFragment : BaseFragment(R.layout.fragment_debug) {
    private val binding by viewBinding(FragmentDebugBinding::bind)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        lifecycleScope.launchWhenStarted {
            while (isActive) {
                updateView()
                delay(1000)
            }
        }
    }

    private fun updateView() {
        val ipv8 = getIpv8()
        val demo = getDemoCommunity()
        binding.txtBootstrap.text = Community.DEFAULT_ADDRESSES.joinToString("\n")
        binding.txtLanAddress.text = demo.myEstimatedLan.toString()
        binding.txtWanAddress.text = demo.myEstimatedWan.toString()
        binding.txtPeerId.text = ipv8.myPeer.mid
        binding.txtPublicKey.text = ipv8.myPeer.publicKey.keyToBin().toHex()
        binding.txtOverlays.text = ipv8.overlays.values.toList().joinToString("\n") {
            it.javaClass.simpleName + " (" + it.getPeers().size + " peers)"
        }

        lifecycleScope.launch {
            val blockCount = withContext(Dispatchers.IO) {
                getTrustChainCommunity().database.getAllBlocks().size
            }
            binding.txtBlockCount.text = blockCount.toString()
        }

        lifecycleScope.launch {
            val chainLength = withContext(Dispatchers.IO) {
                getTrustChainCommunity().getChainLength()
            }
            binding.txtChainLength.text = chainLength.toString()
        }
    }
}
