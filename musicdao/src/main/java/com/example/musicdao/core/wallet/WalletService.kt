package com.example.musicdao.core.wallet

import android.util.Log
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.withContext
import org.bitcoinj.core.*
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

    val userTransactions: MutableStateFlow<List<UserWalletTransaction>> = MutableStateFlow(listOf())

    init {
        BriefLogFormatter.initWithSilentBitcoinJ()

        app = object : WalletAppKit(config.networkParams, config.cacheDir, config.filePrefix) {
            override fun onSetupCompleted() {
                if (wallet().keyChainGroupSize < 1) {
                    val key = ECKey()
                    wallet().importKey(key)
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
                setWalletReceiveListener()
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

    fun wallet(): Wallet {
        return app.wallet()
    }

    fun isStarted(): Boolean {
        return started && app.wallet() != null
    }

    /**
     * Convert an amount of coins represented by a user input string, and then send it
     * @param coinsAmount the amount of coins to send, as a string, such as "5", "0.5"
     * @param publicKey the public key address of the cryptocurrency wallet to send the funds to
     */
    fun sendCoins(publicKey: String, coinsAmount: String) {
        Log.d("MusicDao", "Wallet (1): sending $coinsAmount to $publicKey")

        val coins: BigDecimal = try {
            BigDecimal(coinsAmount.toDouble())
        } catch (e: NumberFormatException) {
            null
        } ?: return

        val satoshiAmount = (coins * SATS_PER_BITCOIN).toLong()

        val targetAddress: Address = try {
            Address.fromString(config.networkParams, publicKey)
        } catch (e: Exception) {
            null
        } ?: return

        val sendRequest = SendRequest.to(targetAddress, Coin.valueOf(satoshiAmount))

        try {
            app.wallet().sendCoins(sendRequest)
            Log.d("MusicDao", "Wallet (2): successfully sent $coinsAmount to $publicKey")
        } catch (e: Exception) {
            Log.d("MusicDao", "Wallet (3): failed sending $coinsAmount to $publicKey")
        }
    }

    /**
     * Query the faucet to the default protocol address
     * @return whether request was successfully or not
     */
    suspend fun defaultFaucetRequest(): Boolean {
        return requestFaucet(protocolAddress().toString())
    }

    /**
     * Query the bitcoin faucet for some starter bitcoins
     * @param address the address to send the coins to
     * @return whether request was successfully or not
     */
    private suspend fun requestFaucet(address: String): Boolean {
        Log.d("MusicDao", "requestFaucet (1): $address")
        val obj = URL("${config.regtestFaucetEndPoint}/addBTC?address=$address")

        return withContext(Dispatchers.IO) {
            try {
                val con: InputStream? = obj.openStream()
                con?.close()
                Log.d("MusicDao", "requestFaucet (2): $address using ${config.regtestFaucetEndPoint}/addBTC?address=$address")
                true
            } catch (exception: IOException) {
                exception.printStackTrace()
                Log.d("MusicDao", "requestFaucet failed (3): $address using ${config.regtestFaucetEndPoint}/addBTC?address=$address")
                Log.d("MusicDao", "requestFaucet failed (4): $exception")
                false
            }
        }
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

    /**
     * @return default address used for all interactions on chain
     */
    fun protocolAddress(): Address {
        return app.wallet().issuedReceiveAddresses[0]
    }

    fun confirmedBalance(): String? {
        return try {
            app.wallet().balance.toFriendlyString()
        } catch (e: java.lang.Exception) {
            null
        }
    }

    fun walletTransactions(): List<UserWalletTransaction> {
        return app.wallet().walletTransactions.map {
            UserWalletTransaction(
                transaction = it.transaction,
                value = it.transaction.getValue(app.wallet()),
                date = it.transaction.updateTime
            )
        }.sortedBy { it.date }
    }

    fun setWalletReceiveListener() {
        userTransactions.value = walletTransactions()
        app.wallet().addCoinsReceivedEventListener { _, _, _, _ ->
            userTransactions.value = walletTransactions()
        }
    }

    fun estimatedBalance(): String? {
        return try {
            app.wallet().getBalance(Wallet.BalanceType.ESTIMATED).toFriendlyString()
        } catch (e: java.lang.Exception) {
            null
        }
    }

    companion object {
        val SATS_PER_BITCOIN = BigDecimal(100_000_000)
    }
}

data class UserWalletTransaction(
    val transaction: Transaction,
    val value: Coin,
    val date: Date
)
