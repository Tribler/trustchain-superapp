package nl.tudelft.ipv8.android.demo.ui.debug

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import kotlinx.android.synthetic.main.fragment_debug.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import nl.tudelft.ipv8.Community
import nl.tudelft.ipv8.android.demo.R
import nl.tudelft.ipv8.android.demo.ui.BaseFragment
import nl.tudelft.ipv8.util.toHex

class DebugFragment : BaseFragment() {
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_debug, container, false)
    }

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
        val demo = getDemoCommunity()
        txtBootstrap.text = Community.DEFAULT_ADDRESSES.joinToString("\n")
        txtLanAddress.text = demo.myEstimatedLan.toString()
        txtWanAddress.text = demo.myEstimatedWan.toString()
        txtPeerId.text = demo.myPeer.mid
        txtPublicKey.text = demo.myPeer.publicKey.keyToBin().toHex()
        txtOverlays.text = ipv8.getOverlays().joinToString("\n") {
            it.javaClass.simpleName + " (" + it.getPeers().size + " peers)"
        }
    }
}
