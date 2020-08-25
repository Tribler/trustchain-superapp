package com.example.musicdao.wallet

import android.content.Context
import android.widget.Toast
import nl.tudelft.ipv8.IPv8
import nl.tudelft.ipv8.keyvault.PrivateKey
import org.bitcoinj.core.Address
import org.bitcoinj.core.Coin
import org.bitcoinj.core.ECKey
import org.bitcoinj.core.listeners.DownloadProgressTracker
import org.bitcoinj.kits.WalletAppKit
import org.bitcoinj.params.MainNetParams
import org.bitcoinj.utils.BriefLogFormatter
import org.bitcoinj.wallet.SendRequest
import java.util.*

class WalletService(val androidContext: Context, val iPv8: IPv8, val privateKey: PrivateKey) {
    val app: WalletAppKit
    val params = MainNetParams.get()
    val filePrefix = "forwarding-service"
    val walletDir = androidContext.cacheDir
    var percentageSynced = 0

    init {
        BriefLogFormatter.initWithSilentBitcoinJ()
        app = object : WalletAppKit(params, walletDir, filePrefix) {
            override fun onSetupCompleted() {
                // Make a fresh new key if no keys in stored wallet.
                if (wallet().keyChainGroupSize < 1) {
                    wallet().importKey(ECKey())
                }

                wallet().addCoinsReceivedEventListener { w, tx, _, _ ->
                    val value: Coin = tx.getValueSentToMe(w)
                    Toast.makeText(androidContext, "Received coins: ${value.toFriendlyString()}", Toast.LENGTH_SHORT).show()
                }
                wallet().addCoinsSentEventListener { w, tx, _, _ ->
                    val value: Coin = tx.getValueSentFromMe(w)
                    Toast.makeText(androidContext, "Sent coins: ${value.toFriendlyString()}", Toast.LENGTH_SHORT).show()
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
        app.startAsync()
    }

    fun status(): String {
        return "Wallet status: " + app.state().name
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
            Toast.makeText(androidContext, "Could not resolve wallet address of peer", Toast.LENGTH_LONG).show()
            return
        }
        val sendRequest = SendRequest.to(targetAddress, Coin.valueOf(satoshiAmount))
        try {
            app.wallet().sendCoins(sendRequest)
            Toast.makeText(androidContext, "Submitted transaction", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Toast.makeText(androidContext, "Error creating transaction (do you have sufficient funds?)", Toast.LENGTH_LONG).show()
        }
    }
}
