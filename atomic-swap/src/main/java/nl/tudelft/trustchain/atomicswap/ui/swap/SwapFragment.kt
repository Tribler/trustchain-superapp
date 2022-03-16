package nl.tudelft.trustchain.atomicswap.ui.swap

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.core.widget.addTextChangedListener
import androidx.lifecycle.ViewModelProvider
import nl.tudelft.trustchain.atomicswap.R
import nl.tudelft.trustchain.atomicswap.databinding.FragmentAtomicSwapBinding
import nl.tudelft.trustchain.common.ui.BaseFragment
import java.math.BigDecimal


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

        val fromCurrencyInput = binding.fromCurrencyInput
        val toCurrencyInput = binding.toCurrencyInput

        val createSwapOfferButton = binding.createSwapOfferButton

        model.createSwapOfferEnabled.observe(viewLifecycleOwner) {
            createSwapOfferButton.isEnabled = it
        }

        ArrayAdapter.createFromResource(
            requireContext(),
            R.array.currency_codes,
            android.R.layout.simple_spinner_item
        ).also { adapter ->
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            fromCurrencySpinner.adapter = adapter
            toCurrencySpinner.adapter = adapter
        }

        fromCurrencyInput.addTextChangedListener { validateInput() }
        toCurrencyInput.addTextChangedListener { validateInput() }

        createSwapOfferButton.setOnClickListener { createSwapOffer() }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
        _model = null
    }

    private fun validateInput() {
        val fromCurrencyAmount = binding.fromCurrencyInput.text.toString().toBigDecimalOrNull()
        val toCurrencyAmount = binding.toCurrencyInput.text.toString().toBigDecimalOrNull()

        model.setSwapOfferEnabled(
            fromCurrencyAmount != null && fromCurrencyAmount > BigDecimal.ZERO
                && toCurrencyAmount != null && toCurrencyAmount > BigDecimal.ZERO
        )
    }

    private fun createSwapOffer() {
        val fromCurrency = binding.fromCurrencySpinner.selectedItem
        val toCurrency = binding.toCurrencySpinner.selectedItem

        // Already validated before
        val fromCurrencyAmount = binding.fromCurrencyInput.text.toString().toBigDecimal()
        val toCurrencyAmount = binding.toCurrencyInput.text.toString().toBigDecimal()

        // TODO Implement making swap offer
        val input = "$fromCurrencyAmount $fromCurrency -> $toCurrencyAmount $toCurrency"
        Toast.makeText(requireContext(), input, Toast.LENGTH_SHORT).show()
    }

}
