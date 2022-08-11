package nl.tudelft.trustchain.common.ebsi

import android.content.Context
import id.walt.services.keystore.KeyStoreService

class AndroidKeyStoreService(context: Context): KeyStoreService() {
    init {
        context.dataDir
    }
}
