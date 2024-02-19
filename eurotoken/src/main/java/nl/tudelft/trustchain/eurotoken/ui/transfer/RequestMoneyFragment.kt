package nl.tudelft.trustchain.eurotoken.ui.transfer

import android.os.Bundle
import android.view.View
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import nl.tudelft.trustchain.common.util.QRCodeUtils
import nl.tudelft.trustchain.common.util.viewBinding
import nl.tudelft.trustchain.eurotoken.R
import nl.tudelft.trustchain.eurotoken.databinding.FragmentRequestMoneyBinding
import nl.tudelft.trustchain.eurotoken.ui.EurotokenBaseFragment

class RequestMoneyFragment : EurotokenBaseFragment(R.layout.fragment_request_money) {
    private val binding by viewBinding(FragmentRequestMoneyBinding::bind)

    private val qrCodeUtils by lazy {
        QRCodeUtils(requireContext())
    }

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?
    ) {
        super.onViewCreated(view, savedInstanceState)

        val json = requireArguments().getString(ARG_DATA)!!

        binding.txtRequestData.text = json
        lifecycleScope.launch {
            val bitmap =
                withContext(Dispatchers.Default) {
                    qrCodeUtils.createQR(json)
                }
            binding.qr.setImageBitmap(bitmap)
        }

        binding.btnContinue.setOnClickListener {
            findNavController().navigate(R.id.action_requestMoneyFragment_to_transactionsFragment)
        }
    }

    companion object {
        const val ARG_DATA = "data"
    }
}
