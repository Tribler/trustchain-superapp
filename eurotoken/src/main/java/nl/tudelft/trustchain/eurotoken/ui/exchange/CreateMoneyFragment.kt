package nl.tudelft.trustchain.eurotoken.ui.exchange

import android.os.Build
import android.os.Bundle
import android.view.View
import androidx.annotation.RequiresApi
import androidx.navigation.fragment.findNavController
import nl.tudelft.ipv8.keyvault.defaultCryptoProvider
import nl.tudelft.ipv8.util.hexToBytes
import nl.tudelft.ipv8.util.toHex
import nl.tudelft.trustchain.common.eurotoken.GatewayStore
import nl.tudelft.trustchain.common.eurotoken.TransactionRepository
import nl.tudelft.trustchain.common.util.viewBinding
import nl.tudelft.trustchain.eurotoken.R
import nl.tudelft.trustchain.eurotoken.community.EuroTokenCommunity
import nl.tudelft.trustchain.eurotoken.databinding.FragmentCreateMoneyBinding
import nl.tudelft.trustchain.eurotoken.ui.EurotokenBaseFragment

class CreateMoneyFragment : EurotokenBaseFragment(R.layout.fragment_create_money) {

    private var addGateway = false
    private var setPreferred = false

    private val binding by viewBinding(FragmentCreateMoneyBinding::bind)

    private val gatewayStore by lazy {
        GatewayStore.getInstance(requireContext())
    }

    private val ownPublicKey by lazy {
        defaultCryptoProvider.keyFromPublicBin(
            transactionRepository.trustChainCommunity.myPeer.publicKey.keyToBin().toHex()
                .hexToBytes()
        )
    }

    private fun getEuroTokenCommunity(): EuroTokenCommunity {
        return getIpv8().getOverlay()
            ?: throw java.lang.IllegalStateException("EuroTokenCommunity is not configured")
    }


    @RequiresApi(Build.VERSION_CODES.O)
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val publicKey = requireArguments().getString(ARG_PUBLIC_KEY)!!
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
        binding.txtGatewayPublicKey.text = publicKey

        binding.btnSend.setOnClickListener {
            val newName = binding.newGatewayName.text.toString()
            if (addGateway && newName.isNotEmpty()) {
                GatewayStore.getInstance(requireContext())
                    .addGateway(key, newName, ip, port.toLong(), setPreferred)
            } else if (setPreferred && gateway != null) {
                GatewayStore.getInstance(requireContext()).setPreferred(gateway)
            }
            getEuroTokenCommunity().connectToGateway(publicKey, ip, port, payment_id)
            findNavController().navigate(R.id.action_createMoneyFragment_to_transactionsFragment)
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
