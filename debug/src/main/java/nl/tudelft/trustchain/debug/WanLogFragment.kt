package nl.tudelft.trustchain.debug

import android.os.Bundle
import android.view.View
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.delay
import nl.tudelft.ipv8.peerdiscovery.DiscoveryCommunity
import nl.tudelft.trustchain.common.ui.BaseFragment
import nl.tudelft.trustchain.common.util.viewBinding
import nl.tudelft.trustchain.debug.databinding.FragmentWanLogBinding
import java.text.SimpleDateFormat

class WanLogFragment : BaseFragment(R.layout.fragment_wan_log) {
    private val binding by viewBinding(FragmentWanLogBinding::bind)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        lifecycleScope.launchWhenStarted {
            while (true) {
                val discovery = getIpv8().getOverlay<DiscoveryCommunity>()!!
                val df = SimpleDateFormat.getTimeInstance(SimpleDateFormat.MEDIUM)
                binding.txtLog.text =
                    "Time      Sender               My WAN\n" + discovery.network.wanLog.getLog()
                        .map {
                            "" + df.format(it.timestamp) + ": " + it.sender.toString()
                                .padEnd(20) + " " + it.wan
                        }.joinToString("\n")
                delay(1000)
            }
        }
    }
}
