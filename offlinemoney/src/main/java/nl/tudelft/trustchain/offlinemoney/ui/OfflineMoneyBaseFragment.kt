package nl.tudelft.trustchain.offlinemoney.ui

import android.os.Bundle
import androidx.lifecycle.lifecycleScope
import mu.KotlinLogging
import nl.tudelft.trustchain.common.ui.BaseFragment

open class OfflineMoneyBaseFragment(contentLayoutId: Int = 0) : BaseFragment(contentLayoutId) {
    protected val logger = KotlinLogging.logger {}

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)


        setHasOptionsMenu(true)
        lifecycleScope.launchWhenResumed {
        }
    }
}
