package nl.tudelft.ipv8.android.demo.ui

import androidx.fragment.app.Fragment
import nl.tudelft.ipv8.IPv8
import nl.tudelft.ipv8.android.demo.DemoApplication
import nl.tudelft.ipv8.android.demo.DemoCommunity

import nl.tudelft.ipv8.android.demo.TrustChainExplorer
import nl.tudelft.ipv8.android.demo.startIfNotRunning
import nl.tudelft.ipv8.attestation.trustchain.TrustChainCommunity

abstract class BaseFragment : Fragment() {
    protected val trustchain: TrustChainExplorer by lazy {
        TrustChainExplorer(getTrustChainCommunity())
    }

    protected fun getIpv8(): IPv8 {
        val ipv8 = (requireContext().applicationContext as DemoApplication).ipv8
        ipv8.startIfNotRunning(requireContext())
        return ipv8
    }

    protected fun getTrustChainCommunity(): TrustChainCommunity {
        val overlays = getIpv8().getOverlays()
        return overlays[TrustChainCommunity::class.java] as? TrustChainCommunity
            ?: throw IllegalStateException("TrustChainCommunity is not configured")
    }

    protected fun getDemoCommunity(): DemoCommunity {
        val overlays = getIpv8().getOverlays()
        return overlays[DemoCommunity::class.java] as? DemoCommunity
            ?: throw IllegalStateException("DemoCommunity is not configured")
    }
}
