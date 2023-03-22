package nl.tudelft.trustchain.offlinemoney.ui

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import kotlinx.coroutines.launch
import nl.tudelft.ipv8.keyvault.PrivateKey
import nl.tudelft.ipv8.keyvault.PublicKey
import nl.tudelft.trustchain.common.util.viewBinding
import nl.tudelft.trustchain.offlinemoney.R
import nl.tudelft.trustchain.offlinemoney.databinding.SendAmountFragmentBinding

class SendAmountFragment : OfflineMoneyBaseFragment(R.layout.send_amount_fragment) {
    private val binding by viewBinding(SendAmountFragmentBinding::bind)

    private val avail1Euro = 5
    private var count1Euro = 0

    @SuppressLint("SetTextI18n")
    private fun updateTextViewBill(txtView: TextView, count: Int, avail: Int) {
        txtView.text = "$count/$avail"
        updateTextViewAmount()
    }

    private fun updateTextViewAmount() {
        var sum: Int = 0;

        sum += count1Euro * 1;

        binding.txtAmount.text = sum.toString()
    }

    @SuppressLint("SetTextI18n")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        updateTextViewBill(binding.txtNumbers, count1Euro, avail1Euro)

//        lifecycleScope.launch {
//            val reqPayload = JSONObject(requireArguments().getString(ARG_RECEIVER)!!)
//
//            binding.txtReceiverInfo.text = "request: $reqPayload"
//        }

        binding.imgPlus.setOnClickListener {
            if(count1Euro < avail1Euro) {
                count1Euro += 1
                updateTextViewBill(binding.txtNumbers, count1Euro, avail1Euro)
            }
        }

        binding.imgMinus.setOnClickListener {
            if(count1Euro > 0) {
                count1Euro -= 1
                updateTextViewBill(binding.txtNumbers, count1Euro, avail1Euro)
            }
        }

        binding.btnCancel.setOnClickListener {
            findNavController().navigate(R.id.action_sendAmountFragment_to_transferFragment)
        }

        binding.btnSend.setOnClickListener{
//            val amount = binding.edtAmount.text.toString().toDouble().toLong()
//
//            val pbk = transactionRepository.myPublicKey
//
//            val pvk = transactionRepository.myPrivateKey
//
//            val reqPayload = RequestPayload.fromJson(JSONObject(requireArguments().getString(ARG_RECEIVER)!!))!!
//            if (amount > 0) {
//                val promise = Promise.createPromise(pbk as PublicKey, reqPayload, amount, s_pvk = pvk)
//
//                val connectionData = promise.toJson()
//
//                val args = Bundle()
//
//                args.putString(SendMoneyFragment.ARG_DATA, connectionData.toString())
//
                findNavController().navigate(
                    R.id.action_sendAmountFragment_to_sendMoneyFragment,
                    //args
                )
//            }

        }
    }

    companion object {
        const val ARG_RECEIVER = "public_key"
    }
}
