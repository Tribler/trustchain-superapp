package nl.tudelft.trustchain.atomicswap.ui.wallet

import android.content.ClipData
import android.content.ClipboardManager
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import nl.tudelft.trustchain.atomicswap.R
import nl.tudelft.trustchain.atomicswap.databinding.FragmentAtomicWalletBinding
import nl.tudelft.trustchain.common.bitcoin.WalletService
import nl.tudelft.trustchain.common.ethereum.EthereumWalletService
import nl.tudelft.trustchain.common.ethereum.EthereumWeb3jWallet
import nl.tudelft.trustchain.common.ui.BaseFragment
import org.bitcoinj.kits.WalletAppKit
import org.bitcoinj.wallet.Wallet
import org.bitcoinj.wallet.listeners.WalletChangeEventListener
import org.web3j.utils.Convert

class WalletFragment : BaseFragment(R.layout.fragment_atomic_wallet), WalletChangeEventListener {

    private var _binding: FragmentAtomicWalletBinding? = null

    private var _model: WalletViewModel? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!
    private val model get() = _model!!

    private lateinit var clipboard: ClipboardManager

    private lateinit var walletAppKit: WalletAppKit
    private lateinit var bitcoinWallet: Wallet

    private lateinit var ethereumWallet: EthereumWeb3jWallet

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        clipboard = ContextCompat.getSystemService(
            requireContext(),
            ClipboardManager::class.java
        ) as ClipboardManager

        walletAppKit = WalletService.getGlobalWallet()
        bitcoinWallet = walletAppKit.wallet()

//        lifecycleScope.launchWhenStarted {
//            ethereumWallet = EthereumWalletService.getGlobalWeb3jWallet(requireContext())
//        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAtomicWalletBinding.inflate(inflater, container, false)
        _model = ViewModelProvider(this).get(WalletViewModel::class.java)

        initializeUi(binding, model)
        return binding.root
    }

    private fun initializeUi(binding: FragmentAtomicWalletBinding, model: WalletViewModel) {
        model.setBitcoinBalance(bitcoinWallet.balance.toFriendlyString())

        val bitcoinBalance = binding.bitcoinBalance
        val ethereumBalance = binding.ethereumBalance

        model.bitcoinBalance.observe(viewLifecycleOwner) {
            bitcoinBalance.text = it
        }

        model.ethereumBalance.observe(viewLifecycleOwner) {
            ethereumBalance.text = it
        }

        binding.bitcoinCopyButton.setOnClickListener {
            val address = bitcoinWallet.currentReceiveAddress().toString()
            clipboard.setPrimaryClip(ClipData.newPlainText("Bitcoin wallet address", address))
            Toast.makeText(requireContext(), "Copied address to clipboard", Toast.LENGTH_SHORT)
                .show()
        }
        binding.ethereumCopyButton.setOnClickListener {
            val address = ethereumWallet.address()
            clipboard.setPrimaryClip(ClipData.newPlainText("Ethereum wallet address", address))
            Toast.makeText(requireContext(), "Copied address to clipboard", Toast.LENGTH_SHORT)
                .show()
        }

        bitcoinWallet.addChangeEventListener(this)
//        lifecycleScope.launchWhenStarted {
//            while (isActive) {
//                val ether = Convert.fromWei(ethereumWallet.balance().toString(), Convert.Unit.ETHER)
//                model.setEthereumBalance("$ether ETH")
//                delay(1000)
//            }
//        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        bitcoinWallet.removeChangeEventListener(this)
        _binding = null
        _model = null
    }

    override fun onWalletChanged(wallet: Wallet) {
        model.setBitcoinBalance(wallet.balance.toFriendlyString())
    }

}
