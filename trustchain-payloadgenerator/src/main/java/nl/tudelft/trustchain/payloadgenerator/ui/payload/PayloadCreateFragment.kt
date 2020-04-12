package nl.tudelft.trustchain.payloadgenerator.ui.payload

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.navigation.findNavController
import kotlinx.android.synthetic.main.fragment_create_payload.*
import nl.tudelft.trustchain.common.ui.BaseFragment
import nl.tudelft.trustchain.payloadgenerator.R

class PayloadCreateFragment : BaseFragment() {
    private var isAsk = false

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_create_payload, container, false)
    }

    @SuppressLint("SetTextI18n")
    @ExperimentalUnsignedTypes
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {

        askBidSwitch.setOnClickListener {
            if (isAsk) {
                txtExplanationAsk.text = "I have BTC, I want Dymbe $"
            } else {
                txtExplanationAsk.text = "I have Dymbe $, I want BTC"
            }
            isAsk = !isAsk
        }

        btnCreatePayload.setOnClickListener {
            val amount = if (editTextAmount.text != null) {
                editTextAmount.text.toString().toDouble()
            } else {
                0.0
            }
            val price = if (editTextPrice.text != null) {
                editTextPrice.text.toString().toDouble()
            } else {
                0.0
            }
            var type = "Bid"
            if (isAsk) {
                type = "Ask"
            }
            val bundle = bundleOf("amount" to amount, "price" to price, "type" to type)
            view.findNavController().navigate(R.id.action_payloadCreateFragment_to_payloadFragment, bundle)
        }
    }
}
