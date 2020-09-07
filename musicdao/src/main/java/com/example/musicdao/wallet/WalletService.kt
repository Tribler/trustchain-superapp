package com.example.musicdao.wallet

import android.widget.Toast
import com.example.musicdao.MusicService
import org.bitcoinj.core.Address
import org.bitcoinj.core.Coin
import org.bitcoinj.core.ECKey
import org.bitcoinj.core.PeerAddress
import org.bitcoinj.core.listeners.DownloadProgressTracker
import org.bitcoinj.kits.WalletAppKit
import org.bitcoinj.params.RegTestParams
import org.bitcoinj.utils.BriefLogFormatter
import org.bitcoinj.wallet.SendRequest
import java.net.InetAddress
import java.net.UnknownHostException
import java.util.*

/**
 * Interaction with a BitcoinJ wallet
 */
class WalletService(val musicService: MusicService) {
    val app: WalletAppKit
    val params = CryptoCurrencyConfig.networkParams
    val filePrefix = CryptoCurrencyConfig.chainFileName
    val walletDir = musicService.applicationContext.cacheDir
    var percentageSynced = 0

    init {
        BriefLogFormatter.initWithSilentBitcoinJ()
        app = object : WalletAppKit(params, walletDir, filePrefix) {
            override fun onSetupCompleted() {
                if (wallet().keyChainGroupSize < 1) {
                    wallet().importKey(ECKey())
                }

                wallet().addCoinsReceivedEventListener { w, tx, _, _ ->
                    val value: Coin = tx.getValueSentToMe(w)
                    musicService.showToast("Received coins: ${value.toFriendlyString()}", Toast.LENGTH_SHORT)
                }
                wallet().addCoinsSentEventListener { w, tx, _, _ ->
                    val value: Coin = tx.getValueSentFromMe(w)
                    musicService.showToast("Sent coins: ${value.toFriendlyString()}", Toast.LENGTH_SHORT)
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
        if (params == RegTestParams.get()) {
            try {
                val localHost = InetAddress.getByName("134.122.59.107")
                app.setPeerNodes(PeerAddress(params, localHost, params.port))
            } catch (e: UnknownHostException) {
                // Borked machine with no loopback adapter configured properly.
                throw RuntimeException(e)
            }
        }
        app.startAsync()
    }

    fun status(): String {
        val status = app.state().name
        if (status == "RUNNING") {
            percentageSynced = 100
        }
        return "Wallet status: $status"
    }

    fun balanceText(): String {
        return "Current balance: " + app.wallet().balance.toFriendlyString()
    }

    fun publicKeyText(): String {
        return "Public key " + app.wallet().currentReceiveAddress().toString()
    }

    fun publicKey(): String {
        return app.wallet().currentReceiveAddress().toString()
    }

    fun sendCoins(publicKey: String, satoshiAmount: Long) {
        val targetAddress: Address?
        try {
            targetAddress = Address.fromString(params, publicKey)
        } catch (e: Exception) {
            musicService.showToast("Could not resolve wallet address of peer", Toast.LENGTH_LONG)
            return
        }
        val sendRequest = SendRequest.to(targetAddress, Coin.valueOf(satoshiAmount))
        try {
            app.wallet().sendCoins(sendRequest)
            musicService.showToast("Submitted transaction", Toast.LENGTH_SHORT)
        } catch (e: Exception) {
            musicService.showToast("Error creating transaction (do you have sufficient funds?)", Toast.LENGTH_SHORT)
        }
    }
}