package nl.tudelft.trustchain.eurotoken.ui.transfer

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import nl.tudelft.trustchain.common.util.QRCodeUtils
import nl.tudelft.trustchain.eurotoken.R
import nl.tudelft.trustchain.eurotoken.databinding.FragmentRequestMoneyBinding
import nl.tudelft.trustchain.eurotoken.ui.EurotokenBaseFragment
import nl.tudelft.trustchain.eurotoken.common.Channel
import androidx.navigation.fragment.navArgs
import nl.tudelft.trustchain.eurotoken.nfc.EuroTokenHCEService
import android.util.Log
import nl.tudelft.ipv8.util.toHex
import org.json.JSONObject
import nl.tudelft.trustchain.common.eurotoken.TransactionRepository
import nl.tudelft.trustchain.common.contacts.ContactStore

class RequestMoneyFragment : EurotokenBaseFragment(R.layout.fragment_request_money) {
    private var _binding: FragmentRequestMoneyBinding? = null

    //    private val walletViewModel: WalletViewModel by activityViewModels()
    private val binding
        get() = _binding!!

    private val navArgs: RequestMoneyFragmentArgs by navArgs()

    private val qrCodeUtils by lazy { QRCodeUtils(requireContext()) }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentRequestMoneyBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val transactionArgs = navArgs.transactionArgs

        if (transactionArgs == null) {
            Toast.makeText(requireContext(), "Error: Request details missing.", Toast.LENGTH_LONG).show()
            findNavController().popBackStack()
            return
        }

        // similar to transferfragment for qr
        val myPublicKey = getTrustChainCommunity().myPeer.publicKey.keyToHash().toHex()
        val myName = ContactStore.getInstance(requireContext()).getContactFromPublicKey(getTrustChainCommunity().myPeer.publicKey)?.name ?: ""
        val amount = transactionArgs.amount
        val jsonData = JSONObject().apply {
            put("amount", amount as Any)
            put("public_key", myPublicKey as Any)
            put("name", myName)
            put("type", "transfer_request")
        }.toString()

        // now enable/disable NFC request button based on channel -> dynamic manner
        if (transactionArgs.channel == Channel.QR) {
            binding.txtIntro.text = "Have the sending party scan this QR code:"
            binding.btnNfcRequest.visibility = View.VISIBLE
            binding.qr.visibility = View.GONE
            binding.txtRequestData.visibility = View.GONE
            binding.txtRequest.visibility = View.GONE
            lifecycleScope.launch {
                val bitmap = withContext(Dispatchers.Default) { qrCodeUtils.createQR(jsonData) }
                binding.qr.setImageBitmap(bitmap)
            }
        } else if (transactionArgs.channel == Channel.NFC) {
            binding.txtIntro.text = "Tap your phone with the sender's phone to receive."
            binding.txtRequest.text = getString(R.string.nfc_activated_for_receiving, TransactionRepository.prettyAmount(amount))
            binding.txtRequest.visibility = View.VISIBLE
            binding.txtRequestData.visibility = View.GONE
            binding.qr.visibility = View.GONE
            binding.btnNfcRequest.visibility = View.GONE
            val payloadBytes = jsonData.toByteArray(Charsets.UTF_8)
            EuroTokenHCEService.setPayload(payloadBytes)
            Log.d(TAG, "NFC HCE Payload set for request: $jsonData")
            Toast.makeText(requireContext(), "NFC active. Ready to be tapped by sender.", Toast.LENGTH_SHORT).show()
        }

        // nfc
        // still static
        binding.btnNfcRequest.setOnClickListener {
            if (transactionArgs.channel == Channel.NFC) {
                val payloadBytes = jsonData.toByteArray(Charsets.UTF_8)
                EuroTokenHCEService.setPayload(payloadBytes)
                Toast.makeText(requireContext(), "NFC Re-activated (Requesting ${TransactionRepository.prettyAmount(amount)})", Toast.LENGTH_LONG).show()
            }
        }
        binding.btnContinue.setOnClickListener {
            if (transactionArgs.channel == Channel.NFC) {
                EuroTokenHCEService.clearPayload()
            }
            findNavController().navigate(R.id.action_requestMoneyFragment_to_transactionsFragment)
        }
    }

    override fun onPause() {
        super.onPause()
        if (view != null && navArgs.transactionArgs?.channel == Channel.NFC) {
            Log.d(TAG, "RequestMoneyFragment onPause, clearing HCE payload for NFC.")
            EuroTokenHCEService.clearPayload()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        if (navArgs.transactionArgs?.channel == Channel.NFC) {
            Log.d(TAG, "RequestMoneyFragment onDestroyView, clearing HCE payload for NFC.")
            EuroTokenHCEService.clearPayload()
        }
        _binding = null
    }

    companion object {
        const val ARG_DATA = "data"
        const val TAG = "RequestMoneyFragment"
    }
}
