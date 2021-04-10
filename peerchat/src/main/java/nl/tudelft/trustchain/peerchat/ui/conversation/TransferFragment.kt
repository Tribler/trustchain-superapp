package nl.tudelft.trustchain.peerchat.ui.conversation

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.EditText
import android.widget.Toast
import androidx.navigation.fragment.findNavController
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import nl.tudelft.ipv8.keyvault.defaultCryptoProvider
import nl.tudelft.ipv8.util.hexToBytes
import nl.tudelft.ipv8.util.toHex
import nl.tudelft.trustchain.common.eurotoken.GatewayStore
import nl.tudelft.trustchain.common.eurotoken.TransactionRepository
import nl.tudelft.trustchain.common.ui.BaseFragment
import nl.tudelft.trustchain.common.util.viewBinding
import nl.tudelft.trustchain.peerchat.R
import nl.tudelft.trustchain.peerchat.community.PeerChatCommunity
import nl.tudelft.trustchain.peerchat.databinding.TransferFragmentBinding

class TransferFragment : BaseFragment(R.layout.transfer_fragment) {

    private val binding by viewBinding(TransferFragmentBinding::bind)

    private fun getPeerChatCommunity(): PeerChatCommunity {
        return getIpv8().getOverlay()
            ?: throw java.lang.IllegalStateException("PeerChatCommunity is not configured")
    }

    private val transactionRepository by lazy {
        TransactionRepository(getTrustChainCommunity(), GatewayStore.getInstance(requireContext()))
    }

    private val publicKeyBin by lazy {
        requireArguments().getString(ARG_PUBLIC_KEY)!!
    }

    private val publicKey by lazy {
        defaultCryptoProvider.keyFromPublicBin(publicKeyBin.hexToBytes())
    }

    private val ownPublicKey by lazy {
        defaultCryptoProvider.keyFromPublicBin(
            getPeerChatCommunity().myPeer.publicKey.keyToBin().toHex().hexToBytes()
        )
    }

    private val name by lazy {
        requireArguments().getString(ARG_NAME)!!
    }

    private val isRequest by lazy {
        requireArguments().getString(ARG_IS_REQUEST)
    }

    private fun sendMoneyMessage(amount: Long, message: String) {
        CoroutineScope(Dispatchers.IO).launch {
            val block = transactionRepository.sendTransferProposalSync(publicKey.keyToBin(), amount)
            if (block == null) {
                Toast.makeText(requireContext(), "Insufficient balance", Toast.LENGTH_LONG).show()
            } else {
                getPeerChatCommunity().sendMessageWithTransaction(message, block.calculateHash(), publicKey)
            }
        }
    }

    private fun requestMoney(amount: Long) {
        val message = "GIB MONEY, $amount"
        if (message.isNotEmpty()) {
            getPeerChatCommunity().sendMessage(message, publicKey)
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val isRequest = requireArguments().getBoolean(ARG_IS_REQUEST)

        binding.txtBalance.text =
            TransactionRepository.prettyAmount(transactionRepository.getMyVerifiedBalance())

        if (isRequest) {
            binding.btnTransfer.text = "Request money"
        } else {
            binding.btnTransfer.text = "Transfer money"
        }

        binding.txtOwnPublicKey.text = ownPublicKey.toString()

        binding.edtAmount.addDecimalLimiter()

        binding.btnTransfer.setOnClickListener {
            val amount = getAmount(binding.edtAmount.text.toString())
            val message = binding.edtMessage.text.toString()
            if (isRequest) {
                requestMoney(amount)
            } else {
                sendMoneyMessage(amount, message)
            }
            findNavController().navigateUp()
        }

        binding.txtContactName.text = name
        binding.txtContactPublicKey.text = publicKey.toString()
        binding.avatar.setUser(publicKey.toString(), name)
    }

    companion object {
        const val ARG_PUBLIC_KEY = "public_key"
        const val ARG_NAME = "name"
        const val ARG_IS_REQUEST = "is_request"

        fun getAmount(amount: String): Long {
            val regex = """[^\d]""".toRegex()
            return regex.replace(amount, "").toLong()
        }

        fun EditText.decimalLimiter(string: String): String {

            var amount = getAmount(string)

            if (amount == 0L) {
                return ""
            }

            // val amount = string.replace("[^\\d]", "").toLong()
            return (amount / 100).toString() + "." + (amount % 100).toString().padStart(2, '0')
        }

        fun EditText.addDecimalLimiter() {

            this.addTextChangedListener(object : TextWatcher {

                override fun afterTextChanged(s: Editable?) {
                    val str = this@addDecimalLimiter.text!!.toString()
                    if (str.isEmpty()) return
                    val str2 = decimalLimiter(str)

                    if (str2 != str) {
                        this@addDecimalLimiter.setText(str2)
                        val pos = this@addDecimalLimiter.text!!.length
                        this@addDecimalLimiter.setSelection(pos)
                    }
                }

                override fun beforeTextChanged(
                    s: CharSequence?,
                    start: Int,
                    count: Int,
                    after: Int
                ) {
                }

                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                }
            })
        }
    }
}
