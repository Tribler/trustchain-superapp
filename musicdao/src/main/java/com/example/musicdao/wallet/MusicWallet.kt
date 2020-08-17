package com.example.musicdao.wallet

import android.content.Context
import com.example.musicdao.R
import kotlinx.android.synthetic.main.fragment_wallet.*
import org.bitcoinj.core.ECKey
import org.bitcoinj.core.listeners.DownloadProgressTracker
import org.bitcoinj.kits.WalletAppKit
import org.bitcoinj.params.MainNetParams
import org.bitcoinj.params.TestNet3Params
import org.bitcoinj.utils.BriefLogFormatter
import java.util.*

class MusicWallet(val context: Context) {
    val app: WalletAppKit
    val params = MainNetParams.get()
    val filePrefix = "forwarding-service"
    val walletDir = context.cacheDir
    var percentageSynced = 0

    init {
        BriefLogFormatter.initWithSilentBitcoinJ()
        app = object : WalletAppKit(params, walletDir, filePrefix) {
            override fun onSetupCompleted() {
                // Make a fresh new key if no keys in stored wallet.
                if (wallet().keyChainGroupSize < 1) {
                    wallet().importKey(ECKey())
                }
            }
        }
    }

    fun startup() {
        app.setDownloadListener(object : DownloadProgressTracker() {
            override fun progress(
                pct: Double,
                blocksSoFar: Int,
                date: Date?
            ) {
                super.progress(pct, blocksSoFar, date)
                percentageSynced = pct.toInt()
            }

            override fun doneDownload() {
                super.doneDownload()
                percentageSynced = 100
            }
        })
        app.setBlockingStartup(false)
            .startAsync()
            .awaitRunning()
    }

    fun status(): String {
        return "Wallet status: " + app.state().name
    }

    fun balanceText(): String {
        return "Current balance: " + app.wallet().balance.toFriendlyString()
    }

    fun publicKey(): String {
        return "Public key " + app.wallet().currentReceiveAddress().toString()
    }
}
