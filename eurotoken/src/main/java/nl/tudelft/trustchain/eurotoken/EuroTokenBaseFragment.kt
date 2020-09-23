package nl.tudelft.trustchain.eurotoken

import androidx.annotation.LayoutRes
import nl.tudelft.trustchain.common.ui.BaseFragment
import nl.tudelft.trustchain.eurotoken.community.EuroTokenCommunity

open class EuroTokenBaseFragment(@LayoutRes contentLayoutId: Int = 0) : BaseFragment(contentLayoutId) {

    protected val eurotoken: EuroTokenCommunity by lazy {
        getEuroTokenCommunity()
    }

    @JvmName("getEuroTokenCommunity1")
    protected fun getEuroTokenCommunity(): EuroTokenCommunity {
        return getIpv8().getOverlay() ?: throw IllegalStateException("EuroTokenCommunity is not configured")
    }
}
