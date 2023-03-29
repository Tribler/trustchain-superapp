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

class SendMoneyFragment : OfflineMoneyBaseFragment(R.layout.send_money_fragment)  {
    private val binding by viewBinding(SendMoneyFragmentBinding::bind)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        //val promiseString = requireArguments().getString(ARG_DATA)!!
        // TO DO to store promise
        //binding.txtSendData.text = promiseString

        val json = JSONObject().put("type", "transfer")
        json.put("payload", 20)

        lifecycleScope.launch {
            val bitmap = withContext(Dispatchers.Default) {
                qrCodeUtils.createQR(json.toString())
            }
            binding.qrImageView.setImageBitmap(bitmap)
        }

        binding.btnContinue.setOnClickListener {
            findNavController().navigate(R.id.action_sendMoneyFragment_to_transferFragment)
        }
    }

    private val qrCodeUtils by lazy {
        QRCodeUtils(requireContext())
    }

    companion object {
        const val ARG_DATA = "data"
    }
}
