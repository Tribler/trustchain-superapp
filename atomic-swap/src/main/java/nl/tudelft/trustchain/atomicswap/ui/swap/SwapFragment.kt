package nl.tudelft.trustchain.atomicswap.ui.swap

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import androidx.lifecycle.ViewModelProvider
import nl.tudelft.trustchain.atomicswap.R
import nl.tudelft.trustchain.atomicswap.databinding.FragmentAtomicSwapBinding
import nl.tudelft.trustchain.common.ui.BaseFragment

class SwapFragment : BaseFragment(R.layout.fragment_atomic_swap) {

    private var _binding: FragmentAtomicSwapBinding? = null

    private var _model: SwapViewModel? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!
    private val model get() = _model!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAtomicSwapBinding.inflate(inflater, container, false)
        _model = ViewModelProvider(this).get(SwapViewModel::class.java)

        initializeUi(binding, model)
        return binding.root
    }

    private fun initializeUi(binding: FragmentAtomicSwapBinding, model: SwapViewModel) {
        val fromCurrencySpinner = binding.fromCurrencySpinner
        val toCurrencySpinner = binding.toCurrencySpinner

        ArrayAdapter.createFromResource(
            requireContext(),
            R.array.currency_codes,
            android.R.layout.simple_spinner_item
        ).also { adapter ->
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            fromCurrencySpinner.adapter = adapter
            toCurrencySpinner.adapter = adapter
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
        _model = null
    }

}
