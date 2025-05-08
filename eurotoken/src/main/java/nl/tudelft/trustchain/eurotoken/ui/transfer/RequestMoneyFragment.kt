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

class RequestMoneyFragment : EurotokenBaseFragment(R.layout.fragment_request_money) {
    private var _binding: FragmentRequestMoneyBinding? = null
    //    private val walletViewModel: WalletViewModel by activityViewModels()
    private val binding
        get() = _binding!!

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

        // Assumed non-null with !! in original
        val json = requireArguments().getString(ARG_DATA)!!

        binding.txtRequestData.text = json
        lifecycleScope.launch {
            val bitmap = withContext(Dispatchers.Default) { qrCodeUtils.createQR(json) }
            binding.qr.setImageBitmap(bitmap)
        }

        binding.btnContinue.setOnClickListener {
            findNavController().navigate(R.id.action_requestMoneyFragment_to_transactionsFragment)
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
