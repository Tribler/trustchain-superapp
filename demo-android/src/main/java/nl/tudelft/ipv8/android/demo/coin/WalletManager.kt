package nl.tudelft.ipv8.android.demo.coin

import android.content.Context
import android.os.Handler
import android.util.Log
import org.bitcoinj.core.ECKey
import org.bitcoinj.core.NetworkParameters
import org.bitcoinj.kits.WalletAppKit
import org.bitcoinj.params.TestNet3Params
import org.bitcoinj.utils.Threading
import java.io.File
import java.net.URI
import java.util.concurrent.Executor

/**
 * The wallet manager which encapsulates the functionality of all possible interactions
 * with bitcoin wallets (including multi-signature wallets).
 * NOTE: Ideally should be separated from any Android UI concepts. Not the case currently.
 */
class WalletManager(walletManagerConfiguration: WalletManagerConfiguration, applicationContext: Context) {
    val params: NetworkParameters
    val filePrefix: String

    init {
        Log.i("Coin", "Coin: WalletManager starting...")

        params = when (walletManagerConfiguration.network) {
            BitcoinNetworkOptions.TEST_NET -> TestNet3Params.get()
            BitcoinNetworkOptions.PRODUCTION -> TestNet3Params.get()
        }

        filePrefix = when (walletManagerConfiguration.network) {
            BitcoinNetworkOptions.TEST_NET -> "forwarding-service-testnet"
            BitcoinNetworkOptions.PRODUCTION -> "forwarding-service"
        }

        // Private location for small-file storage for current app.
        // https://developer.android.com/training/data-storage/app-specific#kotlin
        val storageURI: URI = applicationContext.filesDir.toURI()

        val kit = object : WalletAppKit(params, File(storageURI), filePrefix) {
            override fun onSetupCompleted() {
                // Make a fresh new key if no keys in stored wallet.
                if (wallet().keyChainGroupSize < 1) wallet().importKey(ECKey())
            }
        }

        kit.startAsync()

        setupThread(applicationContext)

        Log.i("Coin", "Coin: WalletManager starting...")
    }

    /**
     * Sets up in which thread BitcoinJ will conduct its background activities.
     */
    fun setupThread(applicationContext: Context) {
        val runInUIThread: Executor = object : Executor {
            override fun execute(runnable: Runnable) {
                val handler = Handler(applicationContext.mainLooper)
                // For Android: handler was created in an Activity.onCreate method.
                handler.post(runnable)
            }
        }

        Threading.USER_THREAD = runInUIThread
    }


}
