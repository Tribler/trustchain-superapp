package nl.tudelft.trustchain.currencyii.ui.bitcoin

import android.app.Dialog
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import nl.tudelft.trustchain.currencyii.R
import nl.tudelft.trustchain.currencyii.coin.BitcoinNetworkOptions
import nl.tudelft.trustchain.currencyii.databinding.ImportKeyDialogBinding

class ImportKeyDialog : DialogFragment() {
    private lateinit var listener: ImportKeyDialogListener

    interface ImportKeyDialogListener {
        fun onImport(
            address: String,
            privateKey: String,
            network: BitcoinNetworkOptions
        )

        fun onImportDone()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        try {
            @Suppress("DEPRECATION")
            listener = targetFragment as ImportKeyDialogListener
        } catch (e: ClassCastException) {
            throw ClassCastException("Calling Fragment must implement Listener")
        }
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return activity?.let {
            val builder = AlertDialog.Builder(it)
            val binding = ImportKeyDialogBinding.inflate(requireActivity().layoutInflater)
            val view = binding.root

            builder.setView(view)
                .setTitle("Import Existing Address")
                .setPositiveButton(
                    "Import"
                ) { _, _ ->

                    val ad = binding.addressInput
                    val sk = binding.privateKeyInput
                    val network = binding.bitcoinNetworksLayout.bitcoinNetworkRadioGroup

                    val address = ad.text.toString()
                    val privateKey = sk.text.toString()

                    val addressValid = isAddressValid(address)
                    val privateKeyValid = isPrivateKeyValid(privateKey)

                    val param =
                        when (network.checkedRadioButtonId) {
                            R.id.production_radiobutton -> BitcoinNetworkOptions.PRODUCTION
                            R.id.testnet_radiobutton -> BitcoinNetworkOptions.TEST_NET
                            R.id.regtest_radiobutton -> BitcoinNetworkOptions.REG_TEST
                            else -> {
                                Toast.makeText(
                                    this.requireContext(),
                                    "Please select a bitcoin network first",
                                    Toast.LENGTH_SHORT
                                ).show()
                                return@setPositiveButton
                            }
                        }
                    if (addressValid && privateKeyValid) {
                        listener.onImport(address, privateKey, param)
                        ad.setText("")
                        sk.setText("")

                        listener.onImportDone()
                        Toast.makeText(
                            this.requireContext(),
                            "Key imported successfully.",
                            Toast.LENGTH_SHORT
                        ).show()
                    } else {
                        Toast.makeText(
                            this.requireContext(),
                            "The address or private key is not formatted correctly.",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
                .setNegativeButton(
                    "Cancel"
                ) { dialog, _ ->
                    dialog.cancel()
                }

            builder.create()
        } ?: throw IllegalStateException("Activity cannot be null")
    }

    private fun isAddressValid(address: String): Boolean {
        return address.length in 26..35
    }

    private fun isPrivateKeyValid(privateKey: String): Boolean {
        return privateKey.length in 51..52 || privateKey.length == 64
    }
}
