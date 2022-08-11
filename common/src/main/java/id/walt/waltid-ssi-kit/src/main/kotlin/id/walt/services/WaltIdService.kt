package id.walt.services

import android.content.Context
import id.walt.servicematrix.BaseService
import nl.tudelft.trustchain.common.ebsi.WaltIdInterface

/**
 * This class extends the service-matrix BaseService in order to run the walt.id specific initialization routines on startup.
 */
abstract class WaltIdService : BaseService() {

    init {
        WaltIdServices
    }

}
