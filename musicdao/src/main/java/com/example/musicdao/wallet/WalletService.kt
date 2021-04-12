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
import java.io.IOException
import java.io.InputStream
import java.math.BigDecimal
import java.net.InetAddress
import java.net.URL
import java.net.UnknownHostException
import java.util.*

/**
 * Interaction with a BitcoinJ wallet
 */
class WalletService(val walletDir: File, private val musicService: MusicService) {
    val app: WalletAppKit
    private val bitcoinFaucetEndpoint = "http://134.122.59.107:3000"
    private val params = CryptoCurrencyConfig.networkParams
    private val filePrefix = CryptoCurrencyConfig.chainFileName
    private var started = false
    var percentageSynced = 0

    init {
        BriefLogFormatter.initWithSilentBitcoinJ()
        app = object : WalletAppKit(params, walletDir, filePrefix) {
            override fun onSetupCompleted() {
                if (wallet().keyChainGroupSize < 1) {
                    val key = ECKey()
                    wallet().importKey(key)
                }

                if (wallet().balance.isZero) {
                    val address = wallet().issuedReceiveAddresses[0].toString()
                    // Ask, using REST call to faucet to get some coins to start with
                    requestStarterCoins(address)
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
        if (params == RegTestParams.get()) {
            try {
                // This is a bootstrap node (a digitalocean droplet, running a full bitcoin regtest
                // node and a miner
                val localHost = InetAddress.getByName("134.122.59.107")
                app.setPeerNodes(PeerAddress(params, localHost, params.port))
            } catch (e: UnknownHostException) {
                // Borked machine with no loopback adapter configured properly.
                throw RuntimeException(e)
            }
        }
        app.startAsync()
        started = true
    }

    fun status(): String {
        val status = app.state().name
        return "Status: $status"
    }

    fun balanceText(): String {
        return try {
            val confirmedBalance = app.wallet().balance.toFriendlyString()
            val estimatedBalance =
                app.wallet().getBalance(Wallet.BalanceType.ESTIMATED).toFriendlyString()
            "Current balance: $confirmedBalance (confirmed) \nCurrent balance: $estimatedBalance (estimated)"
        } catch (e: Exception) {
            e.printStackTrace()
            "Current balance: "
        }
    }

    fun publicKeyText(): String {
        return try {
            "Wallet public key: " + app.wallet().currentReceiveAddress().toString()
        } catch (e: Exception) {
            e.printStackTrace()
            ""
        }
    }

    fun publicKey(): String {
        return try {
            app.wallet().currentReceiveAddress().toString()
        } catch (e: Exception) {
            e.printStackTrace()
            ""
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
            musicService.showToast("Incorrect coins amount given", Toast.LENGTH_SHORT)
            return
        }
        val satoshiAmount = (coins * SATS_PER_BITCOIN).toLong()
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
            musicService.showToast(
                "Sending funds: ${
                Coin.valueOf(satoshiAmount).toFriendlyString()
                }",
                Toast.LENGTH_SHORT
            )
        } catch (e: Exception) {
            musicService.showToast(
                "Error creating transaction (do you have sufficient funds?)",
                Toast.LENGTH_SHORT
            )
        }
    }

    /**
     * Query the bitcoin faucet for some starter bitcoins
     */
    fun requestStarterCoins(id: String) {
        val obj = URL("$bitcoinFaucetEndpoint?id=$id")
        try {
            val con: InputStream? = obj.openStream()
            con?.close()
        } catch (exception: IOException) {
            exception.printStackTrace()
        }
    }

    companion object {
        var walletService: WalletService? = null

        /**
         * Singleton pattern for WalletService
         */
        fun getInstance(walletDir: File, musicService: MusicService): WalletService {
            val instance = walletService
            if (instance is WalletService) return instance
            val newInstance = WalletService(walletDir, musicService)
            walletService = newInstance
            return newInstance
        }

        val SATS_PER_BITCOIN = BigDecimal(100_000_000)
    }
}
