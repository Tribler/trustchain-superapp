package nl.tudelft.trustchain.eurotoken.ui.transfer

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import nl.tudelft.ipv8.keyvault.defaultCryptoProvider
import nl.tudelft.ipv8.util.hexToBytes
import nl.tudelft.ipv8.util.toHex
import nl.tudelft.trustchain.common.ui.BaseFragment
import nl.tudelft.trustchain.common.util.QRCodeUtils
import nl.tudelft.trustchain.common.util.viewBinding
import nl.tudelft.trustchain.eurotoken.R
import nl.tudelft.trustchain.eurotoken.databinding.FragmentRequestMoneyBinding

class RequestMoneyFragment : BaseFragment(R.layout.fragment_request_money) {
    private val binding by viewBinding(FragmentRequestMoneyBinding::bind)

    private val qrCodeUtils by lazy {
        QRCodeUtils(requireContext())
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val json = requireArguments().getString(ARG_DATA)!!

        binding.txtRequestData.text = json
        lifecycleScope.launch {
            val bitmap = withContext(Dispatchers.Default) {
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
