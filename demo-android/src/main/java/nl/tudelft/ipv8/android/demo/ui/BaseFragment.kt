package nl.tudelft.ipv8.android.demo.ui

import androidx.fragment.app.Fragment
import nl.tudelft.ipv8.Ipv8
import nl.tudelft.ipv8.android.demo.DemoCommunity

import nl.tudelft.ipv8.android.demo.Ipv8Application

abstract class BaseFragment : Fragment() {
    protected val ipv8: Ipv8
        get() {
            return (requireContext().applicationContext as Ipv8Application).ipv8
        }

    protected fun getDemoCommunity(): DemoCommunity {
        val overlays = ipv8.getOverlays()
        return overlays.find { it is DemoCommunity } as? DemoCommunity
            ?: throw IllegalStateException("DemoCommunity is not configured")
    }
}
