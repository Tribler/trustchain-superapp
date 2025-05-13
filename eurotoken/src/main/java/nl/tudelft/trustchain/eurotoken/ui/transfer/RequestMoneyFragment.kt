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

        val qrData = transactionArgs.qrData
        // val channel = transactionArgs.channel

        // transactionargs
        if (qrData != null) {
            binding.txtRequestData.text = qrData
            lifecycleScope.launch {
                val bitmap = withContext(Dispatchers.Default) { qrCodeUtils.createQR(qrData) }
                binding.qr.setImageBitmap(bitmap)
            }
        } else {
            Toast.makeText(requireContext(), "Error: QR data for request missing.", Toast.LENGTH_LONG).show()
            findNavController().popBackStack()
            return
        }

        // now enable/disable NFC request button based on channel -> dynamic manner
        if (transactionArgs.channel == Channel.NFC) {
            binding.btnNfcRequest.visibility = View.VISIBLE
            binding.qr.visibility = View.GONE
            binding.txtRequestData.visibility = View.GONE
            binding.txtRequest.visibility = View.GONE
        } else {
            binding.btnNfcRequest.visibility = View.GONE
        }

        // nfc
        // still static
        binding.btnNfcRequest.setOnClickListener {
            // TODO: potentially prepare data??

            Toast.makeText(requireContext(), getString(R.string.receive_via_nfc), Toast.LENGTH_LONG)
                .show()

            // maybe feedback too?
        }
        binding.btnContinue.setOnClickListener { findNavController().popBackStack() }
    }

    override fun onPause() {
        super.onPause()
    }

    override fun onDestroyView() {
        // TODO
        super.onDestroyView()
        _binding = null
    }

    companion object {
        const val ARG_DATA = "data"
        const val TAG = "RequestMoneyFragment"
    }
}
