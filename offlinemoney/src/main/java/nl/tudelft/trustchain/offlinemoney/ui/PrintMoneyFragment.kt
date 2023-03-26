package nl.tudelft.trustchain.offlinemoney.ui

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.View
import androidx.navigation.fragment.findNavController
import nl.tudelft.trustchain.common.util.viewBinding
import nl.tudelft.trustchain.offlinemoney.R
import nl.tudelft.trustchain.offlinemoney.databinding.PrintMoneyFragmentBinding

class PrintMoneyFragment : OfflineMoneyBaseFragment(R.layout.print_money_fragment) {
    private val binding by viewBinding(PrintMoneyFragmentBinding::bind)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.printNumberPicker1.value = 0
        binding.printNumberPicker2.value = 0
        binding.printNumberPicker5.value = 0

        binding.btnCancel.setOnClickListener {
            findNavController().navigate(R.id.action_printMoneyFragment_to_transferFragment)
        }

        binding.btnPrint.setOnClickListener {
            findNavController().navigate(R.id.action_printMoneyFragment_to_transferFragment)
        }
    }
}
