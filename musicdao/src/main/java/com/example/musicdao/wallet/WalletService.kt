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
import org.bitcoinj.wallet.Wallet
import java.io.File
import java.net.InetAddress
import java.net.UnknownHostException
import java.util.*

/**
 * Interaction with a BitcoinJ wallet
 */
class WalletService(val musicService: MusicService) {
    val app: WalletAppKit
    private val params = CryptoCurrencyConfig.networkParams
    private val filePrefix = CryptoCurrencyConfig.chainFileName
    private val walletDir: File = musicService.applicationContext.cacheDir
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
                    if (value != wallet().balance && value != wallet().getBalance(Wallet.BalanceType.ESTIMATED)) {
                        musicService.showToast(
                            "Received coins: ${value.toFriendlyString()}",
                            Toast.LENGTH_SHORT
                        )
                    }
                }
            }
        }
    }

    fun startup() {
        app.setBlockingStartup(false)
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
                // This is a bootstrap node (a digitalocean droplet, running a full bitcoin regtest
                // node and a miner
                val localHost = InetAddress.getByName("167.99.17.227")
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
        val confirmedBalance = app.wallet().balance.toFriendlyString()
        val estimatedBalance =
            app.wallet().getBalance(Wallet.BalanceType.ESTIMATED).toFriendlyString()
        return "Current balance: $confirmedBalance (confirmed) \nCurrent balance: $estimatedBalance (estimated)"
    }

    fun publicKeyText(): String {
        return "Public key " + app.wallet().currentReceiveAddress().toString()
    }

    fun publicKey(): String {
        return try {
            app.wallet().currentReceiveAddress().toString()
        } catch (e: Exception) {
            e.printStackTrace()
            ""
        }
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
            musicService.showToast("Sending funds: ${Coin.valueOf(satoshiAmount).toFriendlyString()}", Toast.LENGTH_SHORT)
        } catch (e: Exception) {
            musicService.showToast(
                "Error creating transaction (do you have sufficient funds?)",
                Toast.LENGTH_SHORT
            )
        }
    }

    companion object {
        var walletService: WalletService? = null

        /**
         * Singleton pattern for WalletService
         */
        fun getInstance(musicService: MusicService): WalletService {
            val instance = walletService
            if (instance is WalletService) return instance
            val newInstance = WalletService(musicService)
            newInstance.startup()
            walletService = newInstance
            return newInstance
        }
    }
}
