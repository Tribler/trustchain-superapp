package nl.tudelft.trustchain.common.ui

import androidx.annotation.LayoutRes
import androidx.fragment.app.Fragment
import nl.tudelft.ipv8.IPv8
import nl.tudelft.ipv8.android.IPv8Android

import nl.tudelft.trustchain.common.util.TrustChainHelper
import nl.tudelft.ipv8.attestation.trustchain.TrustChainCommunity
import nl.tudelft.trustchain.common.DemoCommunity


abstract class BaseFragment(@LayoutRes contentLayoutId: Int = 0) : Fragment(contentLayoutId) {
    protected val trustchain: TrustChainHelper by lazy {
        TrustChainHelper(getTrustChainCommunity())
    }

    protected fun getIpv8(): IPv8 {
        return IPv8Android.getInstance()
    }

    protected fun getTrustChainCommunity(): TrustChainCommunity {
        return getIpv8().getOverlay() ?: throw IllegalStateException("TrustChainCommunity is not configured")
    }

    protected fun getDemoCommunity(): DemoCommunity {
        return getIpv8().getOverlay() ?: throw IllegalStateException("DemoCommunity is not configured")
    }
<<<<<<< HEAD
=======


>>>>>>> 6bb285918f79a3bca60568231d009fb76c0c4141
}
