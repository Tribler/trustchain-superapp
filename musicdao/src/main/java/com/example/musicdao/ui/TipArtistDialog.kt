package com.example.musicdao.ui

import android.app.AlertDialog
import android.app.Dialog
import android.content.DialogInterface
import android.os.Bundle
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.DialogFragment
import com.example.musicdao.MusicService
import com.example.musicdao.R
import com.example.musicdao.util.Util
import org.bitcoinj.core.Address
import org.bitcoinj.params.MainNetParams

class TipArtistDialog(val publicKey: String) : DialogFragment() {
    private val mainNetParams = MainNetParams.get()
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val builder = AlertDialog.Builder(activity)
        // Get the layout inflater
        val inflater = requireActivity().layoutInflater

        val dialogView = inflater.inflate(R.layout.dialog_tip_artist, null)
        val walletService = (activity as MusicService).walletService

        val amountAssistingText = dialog?.findViewById<TextView>(R.id.amountAssistingText)
        amountAssistingText?.text = ""

        builder.setView(dialogView)
            .setPositiveButton("Confirm", DialogInterface.OnClickListener { _, _ ->
                val amountEditText = dialog?.findViewById<EditText>(R.id.amount)
                val amount = amountEditText?.text.toString()
                val satoshiAmount = Integer.parseInt(amount).toLong()
                walletService.sendCoins(publicKey, satoshiAmount)
            }).setNegativeButton("Cancel", DialogInterface.OnClickListener { _, _ ->
            dialog?.cancel()
        })

        return builder.create()
    }
}
