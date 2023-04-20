package nl.tudelft.trustchain.offlinedigitaleuro.ui

import mu.KotlinLogging
import nl.tudelft.trustchain.common.ui.BaseFragment
import nl.tudelft.trustchain.offlinedigitaleuro.db.*

open class OfflineDigitalEuroBaseFragment(contentLayoutId: Int = 0) : BaseFragment(contentLayoutId) {

    val db by lazy { OfflineMoneyRoomDatabase.getDatabase(requireContext()) }

    protected val logger = KotlinLogging.logger {}
}
