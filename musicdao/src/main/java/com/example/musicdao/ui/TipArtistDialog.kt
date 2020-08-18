package com.example.musicdao.ui

import android.app.AlertDialog
import android.app.Dialog
import android.content.DialogInterface
import android.os.Bundle
import android.widget.EditText
import android.widget.Toast
import androidx.fragment.app.DialogFragment
import com.example.musicdao.R
import com.example.musicdao.util.Util

class TipArtistDialog : DialogFragment() {
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val builder = AlertDialog.Builder(activity)
        // Get the layout inflater
        val inflater = requireActivity().layoutInflater

        val dialogView = inflater.inflate(R.layout.dialog_tip_artist, null)

        builder.setView(dialogView)
            .setPositiveButton("Confirm", DialogInterface.OnClickListener { _, _ ->
            }).setNegativeButton("Cancel", DialogInterface.OnClickListener { _, _ ->
            dialog?.cancel()
        })

        return builder.create()
    }
}
