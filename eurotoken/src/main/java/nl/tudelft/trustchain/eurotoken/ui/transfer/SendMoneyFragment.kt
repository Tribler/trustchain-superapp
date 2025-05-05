package nl.tudelft.trustchain.eurotoken.ui.transfer
import androidx.core.os.bundleOf
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.navigation.fragment.findNavController
import nl.tudelft.ipv8.keyvault.defaultCryptoProvider
import nl.tudelft.ipv8.util.hexToBytes
import nl.tudelft.ipv8.util.toHex
import nl.tudelft.trustchain.common.contacts.ContactStore
import nl.tudelft.trustchain.common.eurotoken.TransactionRepository
import nl.tudelft.trustchain.common.util.viewBinding
import nl.tudelft.trustchain.eurotoken.EuroTokenMainActivity
import nl.tudelft.trustchain.eurotoken.R
import nl.tudelft.trustchain.eurotoken.databinding.FragmentSendMoneyBinding
import nl.tudelft.trustchain.eurotoken.ui.EurotokenBaseFragment
import nl.tudelft.trustchain.eurotoken.nfc.EuroTokenHCEService
import android.util.Log
import androidx.activity.result.ActivityResultLauncher
import nl.tudelft.trustchain.eurotoken.ui.NfcReaderActivity
import android.app.Activity
import androidx.activity.result.contract.ActivityResultContracts
import android.view.LayoutInflater
import android.view.ViewGroup
import com.google.android.material.snackbar.Snackbar
import nl.tudelft.trustchain.eurotoken.nfc.NfcError
import nl.tudelft.ipv8.keyvault.PublicKey
import nl.tudelft.trustchain.eurotoken.ui.nfc.NfcResultFragment

class SendMoneyFragment : EurotokenBaseFragment(R.layout.fragment_send_money) {
    private var addContact = false

    private val binding by viewBinding(FragmentSendMoneyBinding::bind)

    private lateinit var nfcReaderLauncher: ActivityResultLauncher<Intent>
    private lateinit var recipientPublicKeyHex: String
    private var transactionAmount: Long = 0L
    private lateinit var recipientKey: PublicKey
    //check if correct TODO

    private val ownPublicKey by lazy {
        defaultCryptoProvider.keyFromPublicBin(
            transactionRepository.trustChainCommunity.myPeer.publicKey.keyToBin().toHex()
                .hexToBytes()
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // how is nfcreaderactivity's result handled??->
        nfcReaderLauncher =
            registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
                Log.d(TAG, "NFC Reader Activity finished with result code: ${result.resultCode}")
                if (result.resultCode == Activity.RESULT_OK) {
                    val receivedData = result.data
                        ?.getStringExtra("nl.tudelft.trustchain.eurotoken.NFC_DATA")
                        ?: return@registerForActivityResult

                    // build the Nav args bundle
                    val bundle = bundleOf("nfcData" to receivedData)

                    // navigate via the NavController
                    findNavController().navigate(
                        R.id.action_sendMoneyFragment_to_nfcResultFragment,
                        bundle
                    )


                } else {
                    val nfcErrorStr = result.data?.getStringExtra("nl.tudelft.trustchain.eurotoken.NFC_ERROR")
                    val errorType = try {
                        nfcErrorStr?.let { NfcError.valueOf(it) } ?: NfcError.UNKNOWN_ERROR
                    } catch (e: IllegalArgumentException) {
                        NfcError.UNKNOWN_ERROR
                    }
                    Log.w(TAG, "NFC Failed or Cancelled: $errorType")
                    val errorMsg = getString(R.string.nfc_confirmation_failed, errorType.name)
                    Toast.makeText(requireContext(), errorMsg, Toast.LENGTH_LONG).show()
                    // TODO: improve error handling? other toast?
                }
            }
    }

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?
    ) {
        super.onViewCreated(view, savedInstanceState)

        val publicKey = requireArguments().getString(ARG_PUBLIC_KEY)!!
        val amount = requireArguments().getLong(ARG_AMOUNT)
        val name = requireArguments().getString(ARG_NAME)!!

        val key = defaultCryptoProvider.keyFromPublicBin(publicKey.hexToBytes())
        val contact = ContactStore.getInstance(view.context).getContactFromPublicKey(key)
        binding.txtContactName.text = contact?.name ?: name

        binding.newContactName.visibility = View.GONE

        if (name.isNotEmpty()) {
            binding.newContactName.setText(name)
        }

        if (contact == null) {
            binding.addContactSwitch.toggle()
            addContact = true
            binding.newContactName.visibility = View.VISIBLE
            binding.newContactName.setText(name)
        } else {
            binding.addContactSwitch.visibility = View.GONE
            binding.newContactName.visibility = View.GONE
        }

        binding.addContactSwitch.setOnClickListener {
            addContact = !addContact
            if (addContact) {
                binding.newContactName.visibility = View.VISIBLE
            } else {
                binding.newContactName.visibility = View.GONE
            }
        }

        val pref =
            requireContext().getSharedPreferences(
                EuroTokenMainActivity.EurotokenPreferences.EUROTOKEN_SHARED_PREF_NAME,
                Context.MODE_PRIVATE
            )
        val demoModeEnabled =
            pref.getBoolean(
                EuroTokenMainActivity.EurotokenPreferences.DEMO_MODE_ENABLED,
                false
            )

        if (demoModeEnabled) {
            binding.txtBalance.text =
                TransactionRepository.prettyAmount(transactionRepository.getMyBalance())
        } else {
            binding.txtBalance.text =
                TransactionRepository.prettyAmount(transactionRepository.getMyVerifiedBalance())
        }
        binding.txtOwnPublicKey.text = ownPublicKey.toString()
        binding.txtAmount.text = TransactionRepository.prettyAmount(amount)
        binding.txtContactPublicKey.text = publicKey

        val trustScore = trustStore.getScore(publicKey.toByteArray())
        logger.info { "Trustscore: $trustScore" }

        if (trustScore != null) {
            if (trustScore >= TRUSTSCORE_AVERAGE_BOUNDARY) {
                binding.trustScoreWarning.text =
                    getString(R.string.send_money_trustscore_warning_high, trustScore)
                binding.trustScoreWarning.setBackgroundColor(
                    ContextCompat.getColor(
                        requireContext(),
                        R.color.android_green
                    )
                )
            } else if (trustScore > TRUSTSCORE_LOW_BOUNDARY) {
                binding.trustScoreWarning.text =
                    getString(R.string.send_money_trustscore_warning_average, trustScore)
                binding.trustScoreWarning.setBackgroundColor(
                    ContextCompat.getColor(
                        requireContext(),
                        R.color.metallic_gold
                    )
                )
            } else {
                binding.trustScoreWarning.text =
                    getString(R.string.send_money_trustscore_warning_low, trustScore)
                binding.trustScoreWarning.setBackgroundColor(
                    ContextCompat.getColor(
                        requireContext(),
                        R.color.red
                    )
                )
            }
        } else {
            binding.trustScoreWarning.text =
                getString(R.string.send_money_trustscore_warning_no_score)
            binding.trustScoreWarning.setBackgroundColor(
                ContextCompat.getColor(
                    requireContext(),
                    R.color.metallic_gold
                )
            )
            binding.trustScoreWarning.visibility = View.VISIBLE
        }


        binding.btnSend.setOnClickListener {
            val newName = binding.newContactName.text.toString()
            if (addContact && newName.isNotEmpty()) {
//                val key = defaultCryptoProvider.keyFromPublicBin(publicKey.hexToBytes())
                ContactStore.getInstance(requireContext())
                    .addContact(key, newName)
            }
            val success = transactionRepository.sendTransferProposal(publicKey.hexToBytes(), amount)
            if (!success) {
                return@setOnClickListener Toast.makeText(
                    requireContext(),
                    "Insufficient balance",
                    Toast.LENGTH_LONG
                ).show()
            }
            findNavController().navigate(R.id.action_sendMoneyFragment_to_transactionsFragment)
        }

        //nfc send
        try {
            binding.btnNfcSend.text = "Start NFC Read"
            binding.btnNfcSend.setOnClickListener {
                Log.d(TAG, "NFC Button clicked. Launching NfcReaderActivity FOR RESULT...")
                val intent = Intent(requireContext(), NfcReaderActivity::class.java)
                nfcReaderLauncher.launch(intent)

            }
            binding.btnNfcSend.visibility = View.VISIBLE

        } catch (e: Exception) {
            Log.e(TAG, "Error setting up btnNfcSend::", e)
        }
    }

    companion object {
        private val TAG = SendMoneyFragment::class.java.simpleName

        const val ARG_AMOUNT = "amount"
        const val ARG_PUBLIC_KEY = "pubkey"
        const val ARG_NAME = "name"
        const val TRUSTSCORE_AVERAGE_BOUNDARY = 70
        const val TRUSTSCORE_LOW_BOUNDARY = 30
    }
}
