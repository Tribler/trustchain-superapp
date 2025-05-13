package nl.tudelft.trustchain.eurotoken.ui.transfer

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.os.bundleOf
import androidx.navigation.fragment.findNavController
import nl.tudelft.ipv8.keyvault.PublicKey
import nl.tudelft.ipv8.keyvault.defaultCryptoProvider
import nl.tudelft.ipv8.util.hexToBytes
import nl.tudelft.ipv8.util.toHex
import nl.tudelft.trustchain.common.contacts.ContactStore
import nl.tudelft.trustchain.common.eurotoken.TransactionRepository
import nl.tudelft.trustchain.common.util.viewBinding
import nl.tudelft.trustchain.eurotoken.EuroTokenMainActivity
import nl.tudelft.trustchain.eurotoken.R
import nl.tudelft.trustchain.eurotoken.common.TransactionArgs
import nl.tudelft.trustchain.eurotoken.databinding.FragmentSendMoneyBinding
import nl.tudelft.trustchain.eurotoken.nfc.NfcError
import nl.tudelft.trustchain.eurotoken.ui.EurotokenBaseFragment
import nl.tudelft.trustchain.eurotoken.ui.NfcReaderActivity
import nl.tudelft.trustchain.common.util.QRCodeUtils
import androidx.navigation.fragment.navArgs
import nl.tudelft.ipv8.Peer
import nl.tudelft.trustchain.eurotoken.common.Channel
import org.json.JSONException
import nl.tudelft.trustchain.eurotoken.community.EuroTokenCommunity
import nl.tudelft.trustchain.eurotoken.common.ConnectionData

class SendMoneyFragment : EurotokenBaseFragment(R.layout.fragment_send_money) {
    private var addContact = false

//    private lateinit var qrCodeUtils: QRCodeUtils
    private val qrCodeUtils by lazy { QRCodeUtils(requireContext()) }

    private val navArgs: SendMoneyFragmentArgs by navArgs()
    private lateinit var currentTransactionArgs: TransactionArgs
    private val binding by viewBinding(FragmentSendMoneyBinding::bind)

    private lateinit var nfcReaderLauncher: ActivityResultLauncher<Intent>
    private lateinit var publicKey: String
    private var transactionAmount: Long = 0L
    private lateinit var recipientKey: PublicKey
    // check if correct TODO

    private val ownPublicKey by lazy {
        defaultCryptoProvider.keyFromPublicBin(
            transactionRepository
                .trustChainCommunity
                .myPeer
                .publicKey
                .keyToBin()
                .toHex()
                .hexToBytes()
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // how is nfcreaderactivity's result handled??->
        nfcReaderLauncher =
            registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result
                ->
                Log.d(
                    TAG,
                    "NFC Reader Activity finished with result code: ${result.resultCode}"
                )
                if (result.resultCode == Activity.RESULT_OK) {
                    val receivedData =
                        result.data?.getStringExtra(
                            "nl.tudelft.trustchain.eurotoken.NFC_DATA"
                        )
                            ?: return@registerForActivityResult

                    // more thoroughly check if this is ok? not sure whether to parse
                    val nfcRecipientPublicKey = receivedData
                    val nfcRecipientName: String? = null

//                    val originalTransactionArgs: TransactionArgs? = arguments?.getParcelable(
//                        TransportChoiceSheet.ARG_TRANSACTION_ARGS,
//                        TransactionArgs::class.java
//                    )
                    val originalTransactionArgs = currentTransactionArgs
                    if (originalTransactionArgs != null) {
                        val updatedTransactionArgs = originalTransactionArgs.copy(
                            publicKey = nfcRecipientPublicKey,
                            name = nfcRecipientName,
                            channel = Channel.NFC
                        )
                        finalizeTransaction(updatedTransactionArgs)
                    } else {
                        Toast.makeText(requireContext(), "NFC scan complete, but original transaction details missing.", Toast.LENGTH_LONG).show()
                    }
                    val bundle = bundleOf("nfcData" to receivedData)

                    findNavController().navigate(R.id.action_sendMoneyFragment_to_nfcResultFragment, bundle)
                } else {
                    val nfcErrorStr =
                        result.data?.getStringExtra(
                            "nl.tudelft.trustchain.eurotoken.NFC_ERROR"
                        )
                    val errorType =
                        try {
                            nfcErrorStr?.let { NfcError.valueOf(it) }
                                ?: NfcError.UNKNOWN_ERROR
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

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val binding = FragmentSendMoneyBinding.bind(view)

        val transactionArgs = navArgs.transactionArgs
        currentTransactionArgs = transactionArgs

        if (transactionArgs == null) {
            Toast.makeText(requireContext(), "Error: Transaction details missing for send.", Toast.LENGTH_LONG).show()
            findNavController().popBackStack()
            return
        }

        binding.btnSend.visibility = View.GONE

        // val publicKeyArg = transactionArgs.publicKey
        val amount = transactionArgs.amount
        var publicKeyArg = transactionArgs.publicKey
        var name = transactionArgs.name
        val channel = transactionArgs.channel

        var recipientKey: PublicKey? = publicKeyArg?.hexToBytes()?.let { defaultCryptoProvider.keyFromPublicBin(it) }
        val contact = recipientKey?.let { ContactStore.getInstance(view.context).getContactFromPublicKey(it) }
        binding.txtContactName.text = contact?.name ?: (name ?: "Unknown Recipient")

        binding.newContactName.visibility = View.GONE

        if (!name.isNullOrEmpty()) {
            binding.newContactName.setText(name, android.widget.TextView.BufferType.EDITABLE)
        }

        if (contact == null) {
            binding.addContactSwitch.toggle()
            addContact = true
            binding.newContactName.visibility = View.VISIBLE
            binding.newContactName.setText(name, android.widget.TextView.BufferType.EDITABLE)
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
            requireContext()
                .getSharedPreferences(
                    EuroTokenMainActivity.EurotokenPreferences
                        .EUROTOKEN_SHARED_PREF_NAME,
                    Context.MODE_PRIVATE
                )
        val demoModeEnabled =
            pref.getBoolean(EuroTokenMainActivity.EurotokenPreferences.DEMO_MODE_ENABLED, false)

        if (demoModeEnabled) {
            binding.txtBalance.text =
                TransactionRepository.prettyAmount(transactionRepository.getMyBalance())
        } else {
            binding.txtBalance.text =
                TransactionRepository.prettyAmount(transactionRepository.getMyVerifiedBalance())
        }
        binding.txtOwnPublicKey.text = ownPublicKey.toString()
        binding.txtAmount.text = TransactionRepository.prettyAmount(amount)
        binding.txtContactPublicKey.text = publicKeyArg ?: ""

        val trustScore = publicKeyArg
            ?.hexToBytes()
            ?.let { trustStore.getScore(it) }
        logger.info { "Trustscore: $trustScore" }

        if (trustScore != null) {
            if (trustScore >= TRUSTSCORE_AVERAGE_BOUNDARY) {
                binding.trustScoreWarning.text =
                    getString(R.string.send_money_trustscore_warning_high, trustScore)
                binding.trustScoreWarning.setBackgroundColor(
                    ContextCompat.getColor(requireContext(), R.color.android_green)
                )
            } else if (trustScore > TRUSTSCORE_LOW_BOUNDARY) {
                binding.trustScoreWarning.text =
                    getString(R.string.send_money_trustscore_warning_average, trustScore)
                binding.trustScoreWarning.setBackgroundColor(
                    ContextCompat.getColor(requireContext(), R.color.metallic_gold)
                )
            } else {
                binding.trustScoreWarning.text =
                    getString(R.string.send_money_trustscore_warning_low, trustScore)
                binding.trustScoreWarning.setBackgroundColor(
                    ContextCompat.getColor(requireContext(), R.color.red)
                )
            }
        } else {
            binding.trustScoreWarning.text =
                getString(R.string.send_money_trustscore_warning_no_score)
            binding.trustScoreWarning.setBackgroundColor(
                ContextCompat.getColor(requireContext(), R.color.metallic_gold)
            )
            binding.trustScoreWarning.visibility = View.VISIBLE
        }

//            val channel = currentTransactionArgs.channel
        val pubKey = currentTransactionArgs.publicKey
        Log.d(TAG, "Channel = $channel, publicKey = $publicKeyArg")

        when (channel) {
            Channel.QR -> {
                Log.d(TAG, "Entering QR branch")
                binding.btnSend.visibility = View.VISIBLE
                // binding.btnSend.visibility = View.GONE
                // no recipient ?-> show a scan button
                binding.btnSend.apply {
                    visibility = View.VISIBLE
                    text = if (pubKey.isNullOrEmpty()) {
                        "Scan Recipient QR"
                    } else {
                        "Send via QR"
                    }
                    setOnClickListener {
                        if (pubKey.isNullOrEmpty()) {
                            qrCodeUtils.startQRScanner(this@SendMoneyFragment)
                        } else {
                            finalizeTransaction(currentTransactionArgs)
                        }
                    }
                }
                //                if (publicKey.isNullOrEmpty()) {
                //                    binding.btnSend.text = "Scan Recipient QR"
                //                    binding.btnSend.setOnClickListener {
                //                        qrCodeUtils.startQRScanner(this)
                //                    }
                //                } else {
                //                    binding.btnSend.text = "Send via QR"
                //                    binding.btnSend.setOnClickListener {
                //                        finalizeTransaction(transactionArgs)
                //                    }
                //                }
            }
            Channel.NFC -> {
                Log.d(TAG, "Entering NFC branch")
                // Channel.NFC -> {
                //                binding.btnSend.visibility = View.VISIBLE
                //
                //
                //                // binding.btnSend.visibility = View.GONE
                //                binding.btnSend.text = "Start NFC Read"
                //                binding.btnSend.setOnClickListener {
                //                    Log.d(TAG, "NFC Button clicked. Launching NfcReaderActivity FOR RESULT...")
                //                    val intent = Intent(requireContext(), NfcReaderActivity::class.java)
                //                    nfcReaderLauncher.launch(intent)
                //                }

                binding.btnSend.apply {
                    text = "Start NFC Read"
                    visibility = View.VISIBLE
                    setOnClickListener {
                        Log.d(TAG, "NFC Button clicked. Launching NfcReaderActivity…")
                        val intent = Intent(requireContext(), NfcReaderActivity::class.java)
                        nfcReaderLauncher.launch(intent)
                    }
                }
                binding.txtContactName.text = "Ready for NFC"
                binding.txtContactPublicKey.text = ""
            }
            else -> {
                Log.w(TAG, "Unknown channel: $channel")
            }
        }

        // else {
        //     binding.trustScoreWarning.text =
        //             getString(R.string.send_money_trustscore_warning_no_score)
        //     binding.trustScoreWarning.setBackgroundColor(
        //             ContextCompat.getColor(requireContext(), R.color.metallic_gold)
        // )
        // binding.trustScoreWarning.visibility = View.VISIBLE
        // }
    }

    // QR code still used old way
    @Deprecated("Using onActivityResult for QR scan…")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        qrCodeUtils.parseActivityResult(requestCode, resultCode, data)
            ?.let { rawQr ->
                onQrScanned(rawQr)
            }
    }

    private fun onQrScanned(qrContent: String) {
        try {
            val cd = ConnectionData(qrContent)
            val updatedArgs = currentTransactionArgs.copy(
                publicKey = cd.publicKey,
                name = cd.name,
                amount = cd.amount
            )
            finalizeTransaction(updatedArgs)
        } catch (e: JSONException) {
            Toast.makeText(
                requireContext(),
                "Scan failed (invalid QR)",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    private fun finalizeTransaction(args: TransactionArgs) {
        val amount = args.amount
        val publicKey = args.publicKey
        val recipientKeyBytes = publicKey?.hexToBytes()
        val recipientKey: PublicKey? = recipientKeyBytes?.let { defaultCryptoProvider.keyFromPublicBin(it) }

        val newName = binding.newContactName.text.toString()
        if (addContact && recipientKey != null && newName.isNotEmpty()) {
            ContactStore.getInstance(requireContext()).addContact(recipientKey, newName)
        }

        if (recipientKey != null) {
            val success = transactionRepository.sendTransferProposal(recipientKey.keyToBin(), amount)
            if (!success) {
                Toast.makeText(
                    requireContext(),
                    "Insufficient balance",
                    Toast.LENGTH_LONG
                ).show()
                return
            }
            // nfc? send address for trust upd
            if (args.channel == Channel.NFC) {
                val peer = findPeer(recipientKey.keyToBin().toHex())
                if (peer != null) {
                    getEuroTokenCommunity().sendAddressesOfLastTransactions(peer)
                } else {
                    Log.w(TAG, "Could not find peer for sending trust addresses after NFC.")
                }
            }
            // QR
            else {
                findNavController().navigate(R.id.action_sendMoneyFragment_to_transactionsFragment)
            }
        } else {
            Toast.makeText(requireContext(), "Recipient public key is missing to send.", Toast.LENGTH_LONG).show()
        }
    }

    private fun findPeer(pubKeyHex: String): Peer? {
        val itr = transactionRepository.trustChainCommunity.getPeers().listIterator()
        while (itr.hasNext()) {
            val cur: Peer = itr.next()
            if (cur.key.keyToBin().toHex() == pubKeyHex) {
                return cur
            }
        }
        return null
    }

    private fun getEuroTokenCommunity(): EuroTokenCommunity {
        return getIpv8().getOverlay<EuroTokenCommunity>()
            ?: throw java.lang.IllegalStateException("EuroTokenCommunity is not configured")
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
