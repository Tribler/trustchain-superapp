package nl.tudelft.trustchain.valuetransfer.ui.walletoverview

import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.lifecycle.lifecycleScope
import nl.tudelft.trustchain.common.eurotoken.GatewayStore
import nl.tudelft.trustchain.common.eurotoken.TransactionRepository
import nl.tudelft.trustchain.common.ui.BaseFragment
import nl.tudelft.trustchain.common.util.QRCodeUtils
import nl.tudelft.trustchain.common.util.viewBinding
import nl.tudelft.trustchain.peerchat.community.PeerChatCommunity
import nl.tudelft.trustchain.valuetransfer.R
import nl.tudelft.trustchain.valuetransfer.databinding.FragmentWalletContactsBinding
import nl.tudelft.trustchain.valuetransfer.databinding.FragmentWalletExchangeBinding
import nl.tudelft.trustchain.valuetransfer.dialogs.TransferMoneyDialog
import nl.tudelft.trustchain.valuetransfer.util.formatBalance

class WalletExchangeFragment : BaseFragment(R.layout.fragment_wallet_exchange) {
    private val binding by viewBinding(FragmentWalletExchangeBinding::bind)

    private val gatewayStore by lazy {
        GatewayStore.getInstance(requireContext())
    }

    private val transactionRepository by lazy {
        TransactionRepository(getIpv8().getOverlay()!!, gatewayStore)
    }

    private fun getPeerChatCommunity(): PeerChatCommunity {
        return getIpv8().getOverlay()
            ?: throw java.lang.IllegalStateException("PeerChatCommunity is not configured")
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        lifecycleScope.launchWhenCreated {
            binding.tvBalanceAmount.text = formatBalance(transactionRepository.getMyVerifiedBalance())
        }

        binding.clTransferQR.setOnClickListener {
            Log.d("TESTJE", "SEND QR CLICKED")

            QRCodeUtils(requireContext()).startQRScanner(this, vertical = true)

        }

        binding.clTransferContact.setOnClickListener {
            Log.d("TESTJE", "SEND INPUT CLICKED")
            TransferMoneyDialog(null, true, transactionRepository, getPeerChatCommunity()).show(parentFragmentManager, tag)
        }

        binding.clRequest.setOnClickListener {
            Log.d("TESTJE", "REQUEST CLICKED")
            TransferMoneyDialog(null, false, transactionRepository, getPeerChatCommunity()).show(parentFragmentManager, tag)
        }

        binding.clButtonBuy.setOnClickListener {
            Log.d("TESTJE", "CLICKED BUY TODO")
        }

        binding.clButtonSell.setOnClickListener {
            Log.d("TESTJE", "CLICKED SELL TODO")
        }
    }

}
