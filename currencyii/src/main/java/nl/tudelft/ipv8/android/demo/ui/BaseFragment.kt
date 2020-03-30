package nl.tudelft.ipv8.android.demo.ui

import androidx.annotation.LayoutRes
import androidx.fragment.app.Fragment
import nl.tudelft.ipv8.IPv8
import nl.tudelft.ipv8.android.IPv8Android
import nl.tudelft.ipv8.android.demo.CoinCommunity
import nl.tudelft.ipv8.android.demo.DemoCommunity

import nl.tudelft.ipv8.android.demo.TrustChainHelper
import nl.tudelft.ipv8.attestation.trustchain.TrustChainCommunity
import nl.tudelft.ipv8.peerdiscovery.DiscoveryCommunity

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

    protected fun getCoinCommunity(): CoinCommunity {
        return getIpv8().getOverlay() ?: throw IllegalStateException("CoinCommunity is not configured")
    }
}
