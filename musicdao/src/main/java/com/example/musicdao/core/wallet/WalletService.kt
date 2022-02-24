package com.example.musicdao.core.wallet

import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
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
import java.io.IOException
import java.io.InputStream
import java.math.BigDecimal
import java.net.InetAddress
import java.net.URL
import java.net.UnknownHostException
import java.util.*

class WalletService(val config: WalletConfig) {
    private var started = false
    private var percentageSynced = 0
    private val app: WalletAppKit

    init {
        BriefLogFormatter.initWithSilentBitcoinJ()

        app = object : WalletAppKit(config.networkParams, config.cacheDir, config.filePrefix) {
            override fun onSetupCompleted() {
                if (wallet().keyChainGroupSize < 1) {
                    val key = ECKey()
                    wallet().importKey(key)
                }
                if (wallet().balance.isZero) {
                    defaultFaucetRequest(amount = "1")
                }
                wallet().addCoinsReceivedEventListener { w, tx, _, _ ->
                    val value: Coin = tx.getValueSentToMe(w)
                    if (value != wallet().balance && value != wallet().getBalance(Wallet.BalanceType.ESTIMATED)) {
//                        musicService.showToast(
//                            "Received coins: ${value.toFriendlyString()}",
//                            Toast.LENGTH_SHORT
//                        )
                    }
                }
            }
        }
    }

    @DelicateCoroutinesApi
    fun start() {
        if (started) return

        app.setBlockingStartup(false)
        app.setDownloadListener(
            object : DownloadProgressTracker() {
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
            }
        )

        if (isRegTest()) {
            try {
                val bootstrap = InetAddress.getByName(config.regtestBootstrapIp)
                app.setPeerNodes(
                    PeerAddress(
                        config.networkParams,
                        bootstrap,
                        config.networkParams.port
                    )
                )
            } catch (e: UnknownHostException) {
                // Borked machine with no loopback adapter configured properly.
                throw RuntimeException(e)
            }
        }

        app.startAsync()
        started = true
    }

    private fun isRegTest(): Boolean {
        return config.networkParams == RegTestParams.get()
    }

    fun walletStatus(): String {
        return app.state().name
    }

    fun percentageSynced(): Int {
        return percentageSynced
    }

    fun confirmedBalance(): String? {
        return try {
            app.wallet().balance.toFriendlyString()
        } catch (e: java.lang.Exception) {
            null
        }
    }

    fun estimatedBalance(): String? {
        return try {
            app.wallet().getBalance(Wallet.BalanceType.ESTIMATED).toFriendlyString()
        } catch (e: java.lang.Exception) {
            null
        }
    }

    fun publicKey(): String? {
        return try {
            app.wallet().currentReceiveAddress().toString()
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Convert an amount of coins represented by a user input string, and then send it
     * @param coinsAmount the amount of coins to send, as a string, such as "5", "0.5"
     * @param publicKey the public key address of the cryptocurrency wallet to send the funds to
     */
    fun sendCoins(publicKey: String, coinsAmount: String) {
        val coins = try {
            BigDecimal(coinsAmount.toDouble())
        } catch (e: NumberFormatException) {
//            musicService.showToast("Incorrect coins amount given", Toast.LENGTH_SHORT)
            return
        }
        val satoshiAmount = (coins * SATS_PER_BITCOIN).toLong()
        val targetAddress: Address?
        try {
            targetAddress = Address.fromString(config.networkParams, publicKey)
        } catch (e: Exception) {
//            musicService.showToast("Could not resolve wallet address of peer", Toast.LENGTH_LONG)
            return
        }
        val sendRequest = SendRequest.to(targetAddress, Coin.valueOf(satoshiAmount))
        try {
            app.wallet().sendCoins(sendRequest)
//            musicService.showToast(
//                "Sending funds: ${
//                Coin.valueOf(satoshiAmount).toFriendlyString()
//                }",
//                Toast.LENGTH_SHORT
//            )
        } catch (e: Exception) {
//            musicService.showToast(
//                "Error creating transaction (do you have sufficient funds?)",
//                Toast.LENGTH_SHORT
//            )
        }
    }

    /**
     * Query the bitcoin faucet for some starter bitcoins
     */
    @DelicateCoroutinesApi
    fun requestFaucet(address: String, amount: String) {
        GlobalScope.launch(Dispatchers.IO) {
            val obj = URL("${config.regtestFaucetEndPoint}/$address/$amount")
            try {
                val con: InputStream? = obj.openStream()
                con?.close()
            } catch (exception: IOException) {
                exception.printStackTrace()
            }
        }
    }

    fun defaultFaucetRequest(amount: String = "1") {
        val address = app.wallet().issuedReceiveAddresses[0].toString()
        requestFaucet(address, amount)
    }

    companion object {
        val SATS_PER_BITCOIN = BigDecimal(100_000_000)
    }
}
