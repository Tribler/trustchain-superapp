package nl.tudelft.trustchain.offlinemoney.ui

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.View
import androidx.navigation.fragment.findNavController
import com.travijuu.numberpicker.library.Enums.ActionEnum
import com.travijuu.numberpicker.library.Interface.ValueChangedListener
import nl.tudelft.trustchain.common.util.viewBinding
import nl.tudelft.trustchain.offlinemoney.R
import nl.tudelft.trustchain.offlinemoney.databinding.SendAmountFragmentBinding


class SendAmountFragment : OfflineMoneyBaseFragment(R.layout.send_amount_fragment) {
    private val binding by viewBinding(SendAmountFragmentBinding::bind)

    private fun updateTextViewAmount() {
        var sum = 0;

        sum += binding.numberPicker1.value * 1 + binding.numberPicker2.value * 2 + binding.numberPicker5.value * 5;

        binding.txtAmount.text = sum.toString()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.numberPicker1.max = 11
        binding.numberPicker2.max = 12
        binding.numberPicker5.max = 15

        binding.numberPicker1.value = 0
        binding.numberPicker2.value = 0
        binding.numberPicker5.value = 0

        class ValueListener : ValueChangedListener {
            override fun valueChanged(value: Int, action: ActionEnum) {
                updateTextViewAmount()
            }
        }

        binding.numberPicker1.valueChangedListener = ValueListener()
        binding.numberPicker2.valueChangedListener = ValueListener()
        binding.numberPicker5.valueChangedListener = ValueListener()

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
