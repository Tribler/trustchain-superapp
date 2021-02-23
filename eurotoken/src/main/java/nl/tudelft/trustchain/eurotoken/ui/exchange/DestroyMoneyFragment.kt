package nl.tudelft.trustchain.eurotoken.ui.exchange

import android.os.Bundle
import android.view.View
import androidx.navigation.fragment.findNavController
import nl.tudelft.ipv8.keyvault.defaultCryptoProvider
import nl.tudelft.ipv8.util.hexToBytes
import nl.tudelft.ipv8.util.toHex
import nl.tudelft.trustchain.common.eurotoken.GatewayStore
import nl.tudelft.trustchain.common.eurotoken.TransactionRepository
import nl.tudelft.trustchain.common.ui.BaseFragment
import nl.tudelft.trustchain.common.util.viewBinding
import nl.tudelft.trustchain.eurotoken.R
import nl.tudelft.trustchain.eurotoken.databinding.FragmentDestroyMoneyBinding
import nl.tudelft.trustchain.eurotoken.ui.EurotokenBaseFragment

class DestroyMoneyFragment : EurotokenBaseFragment(R.layout.fragment_destroy_money) {

    private var addGateway = false
    private var setPreferred = false

    private val binding by viewBinding(FragmentDestroyMoneyBinding::bind)

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
        val payment_id = requireArguments().getString(ARG_PAYMENT_ID)!!
        val ip = requireArguments().getString(ARG_IP)!!
        val port = requireArguments().getInt(ARG_PORT)

        val key = defaultCryptoProvider.keyFromPublicBin(publicKey.hexToBytes())
        val gateway = GatewayStore.getInstance(view.context).getGatewayFromPublicKey(key)
        val hasPref = GatewayStore.getInstance(view.context).getPreferred().isNotEmpty()
        if (!hasPref) {
            binding.swiMakePreferred.toggle()
            setPreferred = true
        }

        binding.txtGatewayName.text = gateway?.name ?: name
        if (gateway?.preferred == true) {
            binding.txtPref.visibility = View.VISIBLE
            binding.swiMakePreferred.visibility = View.GONE
        }

        if (name.isNotEmpty()) {
            binding.newGatewayName.setText(name)
        }

        if (gateway == null) {
            binding.addGatewaySwitch.toggle()
            addGateway = true
            binding.addGatewaySwitch.visibility = View.VISIBLE
            binding.newGatewayName.visibility = View.VISIBLE
        } else {
            if (gateway.preferred) {
                binding.swiMakePreferred.visibility = View.GONE
            }
            binding.addGatewaySwitch.visibility = View.GONE
            binding.newGatewayName.visibility = View.GONE
        }

        binding.swiMakePreferred.setOnClickListener {
            setPreferred = !setPreferred
        }

        binding.addGatewaySwitch.setOnClickListener {
            addGateway = !addGateway
            if (addGateway) {
                binding.newGatewayName.visibility = View.VISIBLE
                binding.swiMakePreferred.visibility = View.VISIBLE
            } else {
                binding.newGatewayName.visibility = View.GONE
                binding.swiMakePreferred.visibility = View.GONE
            }
        }

        binding.txtBalance.text =
            TransactionRepository.prettyAmount(transactionRepository.getMyVerifiedBalance())
        binding.txtOwnPublicKey.text = ownPublicKey.toString()
        binding.txtAmount.text = TransactionRepository.prettyAmount(amount)
        binding.txtGatewayPublicKey.text = publicKey

        binding.btnSend.setOnClickListener {
            val newName = binding.newGatewayName.text.toString()
            if (addGateway && newName.isNotEmpty()) {
                GatewayStore.getInstance(requireContext())
                    .addGateway(key, newName, ip, port.toLong(), setPreferred)
            } else if (setPreferred && gateway != null) {
                GatewayStore.getInstance(requireContext()).setPreferred(gateway)
            }
            transactionRepository.sendDestroyProposal(
                publicKey.hexToBytes(),
                ip,
                port,
                payment_id,
                amount
            )
            findNavController().navigate(R.id.action_destroyMoneyFragment_to_transactionsFragment)
        }

    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
    }

    companion object {
        const val ARG_AMOUNT = "amount"
        const val ARG_PUBLIC_KEY = "public_key"
        const val ARG_NAME = "name"
        const val ARG_PAYMENT_ID = "payment_id"
        const val ARG_IP = "ip"
        const val ARG_PORT = "port"
    }
}
