package com.example.musicdao.ui

import android.app.AlertDialog
import android.app.Dialog
import android.content.DialogInterface
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.widget.EditText
import android.widget.TextView
import androidx.fragment.app.DialogFragment
import com.example.musicdao.MusicService
import com.example.musicdao.R
import org.knowm.xchange.Exchange
import org.knowm.xchange.ExchangeFactory
import org.knowm.xchange.binance.BinanceExchange
import org.knowm.xchange.currency.CurrencyPair
import org.knowm.xchange.service.marketdata.MarketDataService
import java.math.BigDecimal


/**
 * This dialog is shown when the user wants to send a tip to the publisher of a Release, using BTC
 * @param publicKey the Bitcoin wallet public key to send the tip to
 */
class TipArtistDialog(private val publicKey: String) : DialogFragment() {
    var conversionRate = BigDecimal(1.0)
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val builder = AlertDialog.Builder(activity)
        // Get the layout inflater
        val inflater = requireActivity().layoutInflater

        val dialogView = inflater.inflate(R.layout.dialog_tip_artist, null)
        val amountAssistingText = dialogView?.findViewById<TextView>(R.id.amountAssistingText)
        Thread {
            conversionRate = oneUSDInCrypto()
            activity?.runOnUiThread {
                amountAssistingText?.text = "USD (${conversionRate * MBTC_PER_BITCOIN} mBTC)"
            }
        }.start()

        val instructionText = dialogView?.findViewById<TextView>(R.id.instructionText)
        instructionText?.text = "Sending a tip to artist(s), with public key: $publicKey"

        val amountEditText = dialogView?.findViewById<EditText>(R.id.amount)
        amountEditText?.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable) {
                val amount = amountEditText.text.toString()
                try {
                    val value = BigDecimal(Integer.parseInt(amount))
                    val conversionText = conversionRate * value * MBTC_PER_BITCOIN
                    activity?.runOnUiThread {
                        amountAssistingText?.text = "USD ($conversionText mBTC)"
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }

            override fun beforeTextChanged(
                s: CharSequence,
                start: Int,
                count: Int,
                after: Int
            ) {
            }

            override fun onTextChanged(
                s: CharSequence,
                start: Int,
                before: Int,
                count: Int
            ) {
            }
        })

        val walletService = (activity as MusicService).walletService

        builder.setView(dialogView)
            .setPositiveButton("Confirm", DialogInterface.OnClickListener { _, _ ->
                val amount = amountEditText?.text.toString()
                val usdAmount = BigDecimal(Integer.parseInt(amount))
                val satoshiAmount = usdAmount * conversionRate * SATS_PER_BITCOIN
                walletService.sendCoins(publicKey, satoshiAmount.toLong())
            }).setNegativeButton("Cancel", DialogInterface.OnClickListener { _, _ ->
                dialog?.cancel()
            })

        return builder.create()
    }

    private fun oneUSDInCrypto(): BigDecimal {
        val bitstamp: Exchange =
            ExchangeFactory.INSTANCE.createExchange(BinanceExchange::class.java.name)
        val marketDataService: MarketDataService = bitstamp.marketDataService
        val ticker = marketDataService.getTicker(CurrencyPair.BTC_USDT)
        return BigDecimal(1.0).divide(ticker.ask, 7, 0)
    }

    companion object {
        val MBTC_PER_BITCOIN = BigDecimal(1_000)
        val SATS_PER_BITCOIN = BigDecimal(100_000_000)
    }

}
