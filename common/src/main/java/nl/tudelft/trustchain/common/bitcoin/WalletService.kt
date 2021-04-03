package nl.tudelft.trustchain.common.bitcoin

import com.google.common.util.concurrent.Service
import org.bitcoinj.core.ECKey
import org.bitcoinj.core.PeerAddress
import org.bitcoinj.kits.WalletAppKit
import org.bitcoinj.params.RegTestParams
import java.io.File
import java.net.InetAddress
import java.net.URL

class WalletService {

    companion object {
        private lateinit var globalWallet: WalletAppKit
        private val bitcoinFaucetEndpoint = "http://134.122.59.107:3000"
        private val walletStore: MutableMap<String, WalletAppKit> = mutableMapOf()
        val params: RegTestParams = RegTestParams.get()
        private lateinit var lastDir: File

        /**
         * Creates a global bitcoin wallet
         */
        fun createGlobalWallet(dir: File) {
            lastDir = dir
            globalWallet = createWallet(dir, "global")
        }

        /**
         * Returns the global bitcoin wallet, [createGlobalWallet] needs to be called at least once first for the correct cache location
         */
        fun getGlobalWallet(): WalletAppKit {
            globalWallet = createWallet(lastDir, "global")
            return globalWallet
        }

        /**
         * Creates a personal wallet and saves it continuously in the given file. If an app-kit has already
         * started, this function looks up the running app-kit.
         */
        fun createPersonalWallet(dir: File): WalletAppKit =
            createWallet(dir, "personal")

        /**
         * Creates a wallet with the given name and saves it continuously in the given file. If an app-kit
         * has already started, this function looks up the running app-kit and waits for it to be surely
         * running.
         */
        fun createWallet(dir: File, name: String): WalletAppKit {
            // If a wallet app-kit was already stored and not terminated, retrieve it.
            if (walletStore.containsKey(name) &&
                !setOf(
                    Service.State.TERMINATED,
                    Service.State.STOPPING,
                    Service.State.FAILED
                ).contains(walletStore[name]?.state())
            ) {
                walletStore[name]!!.awaitRunning()

                return walletStore[name]!!
            }

            // Create an app-kit with testing bitcoins if empty.
            val app = object : WalletAppKit(params, dir, name) {
                override fun onSetupCompleted() {
                    if (wallet().keyChainGroupSize < 1) {
                        wallet().importKey(ECKey())
                    }

                    if (wallet().balance.isZero) {
                        val address = wallet().issuedReceiveAddresses.first().toString()
                        URL("$bitcoinFaucetEndpoint?id=$address").readBytes()
                    }
                }
            }

            app.setPeerNodes(PeerAddress(params, InetAddress.getByName("134.122.59.107"), params.port))

            app.setAutoSave(true)
            app.setBlockingStartup(false)

            app.startAsync()
            app.awaitRunning()

            // Store the app-kit in the running wallet store
            walletStore[name] = app

            return app
        }
    }
}
