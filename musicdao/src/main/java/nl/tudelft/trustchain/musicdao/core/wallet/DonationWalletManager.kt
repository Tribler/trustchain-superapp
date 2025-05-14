package nl.tudelft.trustchain.musicdao.core.wallet

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import android.content.Context
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import java.util.concurrent.atomic.*
import org.bitcoinj.core.*
import org.bitcoinj.core.listeners.DownloadProgressTracker
import org.bitcoinj.kits.WalletAppKit
import org.bitcoinj.params.RegTestParams
import org.bitcoinj.wallet.*
import java.net.InetAddress
import java.net.UnknownHostException
import java.util.*
import java.util.concurrent.*

const val REG_TEST_FAUCET_IP = "131.180.27.224"

class DonationWalletManager
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
        private val config: WalletConfig
    ) {
        var globalDonationAddress: String = ""
        var progress: Int = 0
        var isDownloading: Boolean = true
        private lateinit var walletKit: WalletAppKit

        val onSetupCompletedListeners = mutableListOf<() -> Unit>()

        fun addOnSetupCompletedListener(listener: () -> Unit) {
            onSetupCompletedListeners.add(listener)
        }

        suspend fun start() =
            withContext(Dispatchers.IO) {
                // Ensure directory exists
                config.cacheDir.mkdirs()

                walletKit =
                    object : WalletAppKit(config.networkParams, config.cacheDir, config.filePrefix) {
                        override fun onSetupCompleted() {
                            // Make a fresh new key if no keys in stored wallet.
                            if (wallet().keyChainGroupSize < 1) {
                                Log.i("DonationWallet", "DonationWallet: Added manually created fresh key")
                                wallet().importKey(ECKey())
                            }
                            wallet().allowSpendingUnconfirmedTransactions()
                            Log.i("DonationWallet", "DonationWallet: DonationWallet started successfully.")
                            onSetupCompletedListeners.forEach {
                                Log.i("DonationWallet", "DonationWallet: calling listener $it")
                                it()
                            }
                        }
                    }

                if (config.networkParams == RegTestParams.get()) {
                    try {
                        val localHost = InetAddress.getByName(REG_TEST_FAUCET_IP)
                        walletKit.setPeerNodes(PeerAddress(config.networkParams, localHost, config.networkParams.port))
                    } catch (e: UnknownHostException) {
                        throw RuntimeException(e)
                    }
                }

                walletKit.setDownloadListener(
                    object : DownloadProgressTracker() {
                        override fun progress(
                            pct: Double,
                            blocksSoFar: Int,
                            date: Date?
                        ) {
                            super.progress(pct, blocksSoFar, date)
                            val percentage = pct.toInt()
                            progress = percentage
                            Log.i("DonationWallet", "Progress: $percentage")
                        }

                        override fun doneDownload() {
                            super.doneDownload()
                            progress = 100
                            Log.i("DonationWallet", "DonationWallet Download Complete!")
                            Log.i("DonationWallet", "DonationWallet Balance: ${walletKit.wallet().balance}")
                            isDownloading = false
                        }
                    }
                )

                walletKit.setBlockingStartup(false)
                    .startAsync()
                    .awaitRunning()

                Log.d("DonationWallet", "Started with address: ${getDonationAddress()}")
            }

        fun getDonationAddress(): String {
            return walletKit.wallet().currentReceiveAddress().toString()
        }
    }
