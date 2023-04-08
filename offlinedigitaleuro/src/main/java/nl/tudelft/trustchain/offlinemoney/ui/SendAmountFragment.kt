package nl.tudelft.trustchain.offlinemoney.ui

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.travijuu.numberpicker.library.Enums.ActionEnum
import com.travijuu.numberpicker.library.Interface.ValueChangedListener
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import nl.tudelft.trustchain.common.util.viewBinding
import nl.tudelft.trustchain.offlinemoney.R
import nl.tudelft.trustchain.offlinemoney.databinding.SendAmountFragmentBinding
import org.json.JSONObject


class SendAmountFragment : OfflineDigitalEuroBaseFragment(R.layout.send_amount_fragment) {
    private val binding by viewBinding(SendAmountFragmentBinding::bind)

    private fun updateSendAmount() {
        var sum = 0;

        sum += binding.numberPicker1.value * 1 + binding.numberPicker2.value * 2 + binding.numberPicker5.value * 5;

        binding.txtAmount.text = sum.toString()
    }

    private fun updateBalance() {
        var sum = 0;

        sum += binding.numberPicker1.max * 1 + binding.numberPicker2.max * 2 + binding.numberPicker5.max * 5;

        binding.txtBalance.text = sum.toString();
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        lifecycleScope.launch(Dispatchers.IO) {
            binding.numberPicker1.max = db.tokensDao().getCountTokensOfValue(1.0)
            binding.numberPicker2.max = db.tokensDao().getCountTokensOfValue(2.0)
            binding.numberPicker5.max = db.tokensDao().getCountTokensOfValue(5.0)
            updateBalance()
        }

        binding.numberPicker1.value = 0
        binding.numberPicker2.value = 0
        binding.numberPicker5.value = 0

        class ValueListener : ValueChangedListener {
            override fun valueChanged(value: Int, action: ActionEnum) {
                updateSendAmount()
            }
        }

        binding.numberPicker1.valueChangedListener = ValueListener()
        binding.numberPicker2.valueChangedListener = ValueListener()
        binding.numberPicker5.valueChangedListener = ValueListener()

        binding.btnCancel.setOnClickListener {
            findNavController().navigate(R.id.action_sendAmountFragment_to_transferFragment)
        }

        binding.btnSend.setOnClickListener{
                val amount = binding.txtAmount.text.toString().toDouble()

                if (amount > 0) {
                    val connectionData = JSONObject()
                    connectionData.put(ARG_5EURO_COUNT, binding.numberPicker5.value)
                    connectionData.put(ARG_2EURO_COUNT, binding.numberPicker2.value)
                    connectionData.put(ARG_1EURO_COUNT, binding.numberPicker1.value)

                    val args = Bundle()

                    args.putString(SendDigitalEuroFragment.ARG_DATA, connectionData.toString())

                    findNavController().navigate(
                        R.id.action_sendAmountFragment_to_sendMoneyFragment,
                        args
                    )
                } else {
                    Toast.makeText(requireContext(), "You need to send an amount bigger than 0", Toast.LENGTH_LONG).show()
                }
        }
    }

    companion object {
        const val ARG_RECEIVER = "public_key"
        const val ARG_5EURO_COUNT = "5euro"
        const val ARG_2EURO_COUNT = "2euro"
        const val ARG_1EURO_COUNT = "1euro"
    }
}
