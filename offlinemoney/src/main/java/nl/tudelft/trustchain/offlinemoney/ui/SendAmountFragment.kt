package nl.tudelft.trustchain.offlinemoney.ui

import android.os.Bundle
import android.view.View
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import nl.tudelft.ipv8.keyvault.PrivateKey
import nl.tudelft.ipv8.util.toHex
import nl.tudelft.trustchain.common.eurotoken.GatewayStore
import nl.tudelft.trustchain.common.eurotoken.TransactionRepository
import nl.tudelft.trustchain.common.util.viewBinding
import nl.tudelft.trustchain.offlinemoney.R
import nl.tudelft.trustchain.offlinemoney.databinding.SendAmountFragmentBinding
import nl.tudelft.trustchain.offlinemoney.payloads.Promise
import nl.tudelft.trustchain.offlinemoney.payloads.RequestPayload
import org.json.JSONObject

class SendAmountFragment : OfflineMoneyBaseFragment(R.layout.send_amount_fragment) {
    private val binding by viewBinding(SendAmountFragmentBinding::bind)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        lifecycleScope.launch {
            val reqPayload = JSONObject(requireArguments().getString(ARG_RECEIVER)!!)

            binding.txtReceiverInfo.text = "request: $reqPayload"
        }

        binding.btnCancel.setOnClickListener {
            findNavController().navigate(R.id.action_sendAmountFragment_to_transferFragment)
        }

        binding.btnSend.setOnClickListener{
            val amount = binding.edtAmount.text.toString().toDouble().toLong()

            val pbk = transactionRepository.trustChainCommunity.myPeer.publicKey

            val pvk = transactionRepository.trustChainCommunity.myPeer.key as PrivateKey

            val reqPayload = RequestPayload.fromJson(JSONObject(requireArguments().getString(ARG_RECEIVER)!!))!!
            if (amount > 0) {
                val promise = Promise.createPromise(pbk, reqPayload, amount, s_pvk = pvk)

                val connectionData = promise.toJson()

                val args = Bundle()

                args.putString(SendMoneyFragment.ARG_DATA, connectionData.toString())

                findNavController().navigate(
                    R.id.action_sendAmountFragment_to_sendMoneyFragment,
                    args
                )
            }

        }
    }

    companion object {
        const val ARG_RECEIVER = "public_key"
    }
}
