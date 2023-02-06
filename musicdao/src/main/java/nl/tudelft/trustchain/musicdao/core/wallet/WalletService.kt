package nl.tudelft.trustchain.musicdao.core.wallet

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.withContext
import org.bitcoinj.core.Address
import org.bitcoinj.core.Coin
import org.bitcoinj.core.Transaction
import org.bitcoinj.core.listeners.DownloadProgressTracker
import org.bitcoinj.kits.WalletAppKit
import org.bitcoinj.wallet.SendRequest
import org.bitcoinj.wallet.Wallet
import java.io.IOException
import java.io.InputStream
import java.math.BigDecimal
import java.net.URL
import java.util.*

class WalletService(val config: WalletConfig, private val app: WalletAppKit) {
    private var started = false
    private var percentageSynced = 0

    val userTransactions: MutableStateFlow<List<UserWalletTransaction>> = MutableStateFlow(listOf())
    val onSetupCompletedListeners = mutableListOf<() -> Unit>()

    fun addOnSetupCompletedListener(listener: () -> Unit) {
        onSetupCompletedListeners.add(listener)
    }

    init {
        app.setDownloadListener(object : DownloadProgressTracker() {
            override fun progress(
                pct: Double,
                blocksSoFar: Int,
                date: Date?
            ) {
                super.progress(pct, blocksSoFar, date)
                val percentage = pct.toInt()
                percentageSynced = percentage
                Log.i("MusicDao2", "Progress: $percentage")
            }

            override fun doneDownload() {
                super.doneDownload()
                percentageSynced = 100
                Log.d("MusicDao2", "Download Complete!")
                Log.d("MusicDao2", "Balance: ${app.wallet().balance}")
            }
        })
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
    fun sendCoins(publicKey: String, coinsAmount: String): Boolean {
        Log.d("MusicDao", "Wallet (1): sending $coinsAmount to $publicKey")

        val coins: BigDecimal = try {
            BigDecimal(coinsAmount.toDouble())
        } catch (e: NumberFormatException) {
            Log.d("MusicDao", "Wallet (2): failed to parse $coinsAmount")
            null
        } ?: return false

        val satoshiAmount = (coins * SATS_PER_BITCOIN).toLong()

        val targetAddress: Address = try {
            Address.fromString(config.networkParams, publicKey)
        } catch (e: Exception) {
            Log.d("MusicDao", "Wallet (3): failed to parse $publicKey")
            null
        } ?: return false

        val sendRequest = SendRequest.to(targetAddress, Coin.valueOf(satoshiAmount))

        return try {
            app.wallet().sendCoins(sendRequest)
            Log.d("MusicDao", "Wallet (2): successfully sent $coinsAmount to $publicKey")
            true
        } catch (e: Exception) {
            Log.d("MusicDao", "Wallet (3): failed sending $coinsAmount to $publicKey")
            false
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
                Log.d(
                    "MusicDao",
                    "requestFaucet (2): $address using ${config.regtestFaucetEndPoint}/addBTC?address=$address"
                )
                true
            } catch (exception: IOException) {
                exception.printStackTrace()
                Log.d(
                    "MusicDao",
                    "requestFaucet failed (3): $address using ${config.regtestFaucetEndPoint}/addBTC?address=$address"
                )
                Log.d("MusicDao", "requestFaucet failed (4): $exception")
                false
            }
        }
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

    fun confirmedBalance(): Coin? {
        return try {
            app.wallet().balance
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
        }.sortedByDescending { it.date }
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
