package nl.tudelft.trustchain.eurotoken.ui.transfer

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.navigation.fragment.findNavController
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import nl.tudelft.trustchain.eurotoken.R
import nl.tudelft.trustchain.eurotoken.databinding.FragmentTransportChoiceBinding
import nl.tudelft.trustchain.eurotoken.common.Mode
import nl.tudelft.trustchain.eurotoken.common.Channel
import nl.tudelft.trustchain.eurotoken.common.TransactionArgs

// TODO maybe adjust naming after functionlaity
// now bottomsheet/transport choice
class TransportChoiceSheet : BottomSheetDialogFragment() {
    private var _binding: FragmentTransportChoiceBinding? = null
    private val binding get() = _binding!!

    private lateinit var originalTransactionArgs: TransactionArgs

    companion object {
        const val ARG_TRANSACTION_ARGS_RECEIVED = "transaction_args"
        fun newInstance(args: TransactionArgs) = TransportChoiceSheet().apply {
            arguments = bundleOf(ARG_TRANSACTION_ARGS_RECEIVED to args)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // still getparceable.. since version incorrect
        originalTransactionArgs = requireArguments()
            .getParcelable(ARG_TRANSACTION_ARGS_RECEIVED)
            ?: throw IllegalStateException("TransactionArgs missing")
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?) =
        FragmentTransportChoiceBinding.inflate(inflater, container, false)
            .also { _binding = it }
            .root

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        when (originalTransactionArgs.mode) {
            Mode.SEND -> {
                binding.txtSheetTitle.text = getString(R.string.title_send_options)
                binding.btnReceiveQr.visibility = View.GONE
                binding.btnReceiveNfc.visibility = View.GONE
                binding.btnSendQr.visibility = View.VISIBLE
                binding.btnSendNfc.visibility = View.VISIBLE
            }
            Mode.RECEIVE -> {
                binding.txtSheetTitle.text = getString(R.string.title_receive_options)
                binding.btnSendQr.visibility = View.GONE
                binding.btnSendNfc.visibility = View.GONE
                binding.btnReceiveQr.visibility = View.VISIBLE
                binding.btnReceiveNfc.visibility = View.VISIBLE
            }
        }

        binding.btnSendQr.setOnClickListener { navigateWithSelectedChannel(Channel.QR) }
        binding.btnSendNfc.setOnClickListener { navigateWithSelectedChannel(Channel.NFC) }
        binding.btnReceiveQr.setOnClickListener { navigateWithSelectedChannel(Channel.QR) }
        binding.btnReceiveNfc.setOnClickListener { navigateWithSelectedChannel(Channel.NFC) }
    }

    private fun navigateWithSelectedChannel(selectedChannel: Channel) {
        val argsForNextFragment = originalTransactionArgs.copy(
            channel = selectedChannel
        )

        val destinationId = when (originalTransactionArgs.mode) {
            Mode.SEND -> R.id.sendMoneyFragment
            Mode.RECEIVE -> R.id.requestMoneyFragment
        }

        val navigationArgs = bundleOf("transaction_args" to argsForNextFragment)

        findNavController().navigate(
            destinationId,
            navigationArgs
        )
        dismiss()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
