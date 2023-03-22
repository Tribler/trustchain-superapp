package nl.tudelft.trustchain.offlinemoney.ui

import android.os.Bundle
import androidx.room.Room
import mu.KotlinLogging
import nl.tudelft.trustchain.common.contacts.ContactStore
import nl.tudelft.trustchain.common.eurotoken.GatewayStore
import nl.tudelft.trustchain.common.eurotoken.TransactionRepository
import nl.tudelft.trustchain.common.ui.BaseFragment
import nl.tudelft.trustchain.offlinemoney.db.OfflineMoneyRoomDatabase
import nl.tudelft.trustchain.offlinemoney.db.UserDao

open class OfflineMoneyBaseFragment(contentLayoutId: Int = 0) : BaseFragment(contentLayoutId) {
    protected val logger = KotlinLogging.logger {}

    protected val transactionRepository by lazy {
        TransactionRepository(getIpv8().getOverlay()!!, gatewayStore)
    }

    private val contactStore by lazy {
        ContactStore.getInstance(requireContext())
    }

    private val gatewayStore by lazy {
        GatewayStore.getInstance(requireContext())
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setHasOptionsMenu(true)

    }

    override fun onResume() {
        super.onResume()
    }

    override fun onPause() {
        super.onPause()
    }

//    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
//        inflater.inflate(R.menu.eurotoken_options, menu)
//        menu.findItem(R.id.toggleDemoMode).setTitle(getDemoModeMenuItemText())
//    }

}
