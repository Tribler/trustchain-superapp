package com.example.musicdao

import androidx.annotation.LayoutRes
import com.example.musicdao.ipv8.MusicCommunity
import nl.tudelft.trustchain.common.ui.BaseFragment

/**
 * An abstract layer between the MusicCommunity and an Android Fragment
 */
abstract class MusicBaseFragment(@LayoutRes contentLayoutId: Int = 0) :
    BaseFragment(contentLayoutId) {
    protected fun getMusicCommunity(): MusicCommunity {
        return getIpv8().getOverlay()
            ?: throw IllegalStateException("MusicCommunity is not configured")
    }
}
