package nl.tudelft.trustchain.offlinemoney.ui

import android.os.Bundle
import android.view.View
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import nl.tudelft.trustchain.common.util.QRCodeUtils
import nl.tudelft.trustchain.common.util.viewBinding
import nl.tudelft.trustchain.offlinemoney.R
import nl.tudelft.trustchain.offlinemoney.databinding.SendMoneyFragmentBinding
import org.json.JSONObject

class SendMoneyFragment  : OfflineMoneyBaseFragment(R.layout.send_money_fragment)  {
    private val binding by viewBinding(SendMoneyFragmentBinding::bind)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)


        // val exchange_list = mutableListOf("public_key" , "private_key", 5);

        val exchange_json_object = requireArguments().getString(ARG_DATA)!!
        println(exchange_json_object)
        binding.txtSendData.text = exchange_json_object

        // binding.requestJsonData.text = exchange_json_object

        lifecycleScope.launch {
            val bitmap = withContext(Dispatchers.Default) {
                qrCodeUtils.createQR(exchange_json_object)
            }
            binding.qrImageView.setImageBitmap(bitmap)
        }

        binding.btnContinue.setOnClickListener {
            findNavController().navigate(R.id.action_sendMoneyFragment_to_tranferFragment)
        }
    }

    private val qrCodeUtils by lazy {
        QRCodeUtils(requireContext())
    }

    companion object {
        const val ARG_DATA = "data"
    }
}
