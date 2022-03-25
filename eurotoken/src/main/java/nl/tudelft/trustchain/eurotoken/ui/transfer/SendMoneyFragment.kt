package nl.tudelft.trustchain.eurotoken.ui.transfer

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.navigation.fragment.findNavController
import kotlinx.android.synthetic.main.fragment_send_money.*
import nl.tudelft.ipv8.Peer
import nl.tudelft.ipv8.keyvault.defaultCryptoProvider
import nl.tudelft.ipv8.util.hexToBytes
import nl.tudelft.ipv8.util.toHex
import nl.tudelft.trustchain.common.contacts.ContactStore
import nl.tudelft.trustchain.common.eurotoken.GatewayStore
import nl.tudelft.trustchain.common.eurotoken.TransactionRepository
import nl.tudelft.trustchain.common.util.viewBinding
import nl.tudelft.trustchain.eurotoken.R
import nl.tudelft.trustchain.eurotoken.databinding.FragmentSendMoneyBinding
import nl.tudelft.trustchain.eurotoken.ui.EurotokenBaseFragment
import kotlin.math.roundToInt

class SendMoneyFragment : EurotokenBaseFragment(R.layout.fragment_send_money) {

    private var addContact = false

    private val binding by viewBinding(FragmentSendMoneyBinding::bind)

    private val gatewayStore by lazy {
        GatewayStore.getInstance(requireContext())
    }

    private val ownPublicKey by lazy {
        defaultCryptoProvider.keyFromPublicBin(
            transactionRepository.trustChainCommunity.myPeer.publicKey.keyToBin().toHex()
                .hexToBytes()
        )
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val publicKey = requireArguments().getString(ARG_PUBLIC_KEY)!!
        val amount = requireArguments().getLong(ARG_AMOUNT)
        val name = requireArguments().getString(ARG_NAME)!!
        val mid = requireArguments().getString(ARG_MID)!!

        val key = defaultCryptoProvider.keyFromPublicBin(publicKey.hexToBytes())
        val contact = ContactStore.getInstance(view.context).getContactFromPublicKey(key)
        binding.txtContactName.text = contact?.name ?: name

        binding.newContactName.visibility = View.GONE

        if (name.isNotEmpty()) {
            binding.newContactName.setText(name)
        }

        if (contact == null) {
            binding.addContactSwitch.toggle()
            addContact = true
            binding.newContactName.visibility = View.VISIBLE
            binding.newContactName.setText(name)
        } else {
            binding.addContactSwitch.visibility = View.GONE
            binding.newContactName.visibility = View.GONE
        }

        binding.addContactSwitch.setOnClickListener {
            addContact = !addContact
            if (addContact) {
                binding.newContactName.visibility = View.VISIBLE
            } else {
                binding.newContactName.visibility = View.GONE
            }
        }

        binding.txtBalance.text =
            TransactionRepository.prettyAmount(transactionRepository.getMyVerifiedBalance())
        binding.txtOwnPublicKey.text = ownPublicKey.toString()
        binding.txtAmount.text = TransactionRepository.prettyAmount(amount)
        binding.txtContactPublicKey.text = publicKey

        val trustScore = trustScores?.get(publicKey)
        logger.info { "Trustscore: $trustScore" }

        if (trustScore != null && trustScore is Double) {
            val trustScorePercentage = (trustScore * 100).roundToInt()
            if (0.3 < trustScore && trustScore < 0.7) {
                trustScoreWarning.text = getString(R.string.send_money_trustscore_warning_average, trustScorePercentage)
                trustScoreWarning.setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.metallic_gold))
                trustScoreWarning.visibility = View.VISIBLE
            } else if (trustScore < 0.3) {
                trustScoreWarning.text = getString(R.string.send_money_trustscore_warning_low, trustScorePercentage)
                trustScoreWarning.setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.red))
                trustScoreWarning.visibility = View.VISIBLE
            } else {
                trustScoreWarning.visibility = View.GONE
            }
        } else {
            trustScoreWarning.text = getString(R.string.send_money_trustscore_warning_no_score)
            trustScoreWarning.setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.metallic_gold))
            trustScoreWarning.visibility = View.VISIBLE
        }

        binding.btnSend.setOnClickListener {
            val newName = binding.newContactName.text.toString()
            if (addContact && newName.isNotEmpty()) {
//                val key = defaultCryptoProvider.keyFromPublicBin(publicKey.hexToBytes())
                ContactStore.getInstance(requireContext())
                    .addContact(key, newName)
            }
            val success = transactionRepository.sendTransferProposal(publicKey.hexToBytes(), amount)
            if(!success) {
                return@setOnClickListener Toast.makeText(
                    requireContext(),
                    "Insufficient balance",
                    Toast.LENGTH_LONG
                ).show()
            }
            findNavController().navigate(R.id.action_sendMoneyFragment_to_transactionsFragment)
        }

    }

    companion object {
        const val ARG_AMOUNT = "amount"
        const val ARG_PUBLIC_KEY = "pubkey"
        const val ARG_NAME = "name"
        const val ARG_MID = "mid"
    }
}
