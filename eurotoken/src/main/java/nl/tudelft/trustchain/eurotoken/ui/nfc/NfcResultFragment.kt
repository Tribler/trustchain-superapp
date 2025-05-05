package nl.tudelft.trustchain.eurotoken.ui.nfc

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import nl.tudelft.trustchain.eurotoken.R
import nl.tudelft.trustchain.eurotoken.databinding.FragmentNfcResultBinding

class NfcResultFragment : Fragment(R.layout.fragment_nfc_result) {

    private var _binding: FragmentNfcResultBinding? = null
    private val binding get() = _binding!!

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentNfcResultBinding.bind(view)

        val receivedData = requireArguments().getString("nfcData")
            ?: "No data received"
        binding.tvNfcDataContent.text = receivedData
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
