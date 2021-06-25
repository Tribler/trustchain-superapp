package nl.tudelft.trustchain.peerchat

import androidx.annotation.LayoutRes
import androidx.fragment.app.Fragment
import nl.tudelft.trustchain.common.ui.BaseFragment
import nl.tudelft.trustchain.peerchat.community.PeerChatCommunity

abstract class PeerChatFragment (@LayoutRes contentLayoutId: Int = 0) : BaseFragment(contentLayoutId){

    protected fun getPeerChatCommunity(): PeerChatCommunity {
        return getIpv8().getOverlay()
            ?: throw java.lang.IllegalStateException("PeerChatCommunity is not configured")
    }
}
