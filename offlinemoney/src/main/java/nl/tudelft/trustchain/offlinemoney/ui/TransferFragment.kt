package nl.tudelft.trustchain.offlinemoney.ui

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.navigation.fragment.findNavController
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import nl.tudelft.ipv8.util.toHex
import nl.tudelft.trustchain.common.util.QRCodeUtils
import nl.tudelft.trustchain.common.util.viewBinding
import nl.tudelft.trustchain.offlinemoney.R
import nl.tudelft.trustchain.offlinemoney.databinding.ActivityMainOfflineMoneyBinding
import nl.tudelft.trustchain.offlinemoney.payloads.RequestPayload
import nl.tudelft.trustchain.offlinemoney.payloads.Promise
import org.json.JSONObject
import org.json.JSONException

class TransferFragment : OfflineMoneyBaseFragment(R.layout.activity_main_offline_money) {
    private val binding by viewBinding(ActivityMainOfflineMoneyBinding::bind)

    private val qrCodeUtils by lazy {
        QRCodeUtils(requireContext())
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.edt1Euro.text = 5.toString()
//        val pbk = transactionRepository.
//
//        lifecycleScope.launch {
//            binding.txtPublicKey.text = pbk.toHex()
//
//            val json = JSONObject().put("type", "request")
//            json.put("payload", pbk.toString())
//            val bitmap = withContext(Dispatchers.Default) {
//                qrCodeUtils.createQR(json.toString())
//            }
//            binding.qrPublicKey.setImageBitmap(bitmap)
//        }

        binding.btnGet.setOnClickListener {
            qrCodeUtils.startQRScanner(this)
        }

        binding.btnSend.setOnClickListener{
            findNavController().navigate(R.id.action_transferFragment_to_sendAmountFragment)
        }

    }

    @Deprecated("Deprecated in Java")
    @SuppressLint("SetTextI18n")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        qrCodeUtils.parseActivityResult(requestCode, resultCode, data)?.let {
            try {
                val type = JSONObject(it).optString("type")
//                if (type == "transfer") {
                    val promise = Promise.fromJson(JSONObject(JSONObject(it).getString("payload")))!!
                    // TO DO to store promise
                    val amount = promise.amount
                    val past = binding.txtBalance.text.toString().toDouble()

                    binding.txtBalance.text = (past + amount).toString()
//                } else {
//                    val args = Bundle()
//                    args.putString(SendAmountFragment.ARG_RECEIVER, JSONObject(it).getString("payload"))
//                    findNavController().navigate(
//                        R.id.action_transferFragment_to_sendAmountFragment,
//                        args
//                    )
//                }
            } catch (e: JSONException) {
                Toast.makeText(requireContext(), "Scan failed, try again", Toast.LENGTH_LONG).show()
            }
        } ?: Toast.makeText(requireContext(), "Scan failed", Toast.LENGTH_LONG).show()
        return
    }
}

