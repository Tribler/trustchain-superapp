package nl.tudelft.trustchain.currencyii.ui.bitcoin

import android.app.Dialog
import android.content.DialogInterface
import android.os.Bundle
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import androidx.navigation.fragment.findNavController
import nl.tudelft.trustchain.currencyii.R

class MyDAOsAddDialog : DialogFragment() {

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return activity?.let {
            val builder = AlertDialog.Builder(it)
            builder.setTitle(R.string.my_daos_join_or_create)
                .setItems(R.array.my_daos_options,
                    DialogInterface.OnClickListener { _, which ->
                        when (which) {
                            0 -> findNavController().navigate(R.id.joinNetworkFragment)
                            1 -> findNavController().navigate(R.id.createSWFragment)
                            else -> dialog?.cancel()
                        }
                    })
            builder.create()
        } ?: throw IllegalStateException("Activity cannot be null")
    }
}
