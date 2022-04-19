package nl.tudelft.trustchain.atomicswap.ui.wallet

import android.app.AlertDialog
import android.content.ClipData
import android.content.ClipboardManager
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import nl.tudelft.ipv8.util.sha256
import nl.tudelft.ipv8.util.toHex
import nl.tudelft.trustchain.atomicswap.BuildConfig
import nl.tudelft.trustchain.atomicswap.R
import nl.tudelft.trustchain.atomicswap.databinding.FragmentAtomicWalletBinding
import nl.tudelft.trustchain.atomicswap.swap.WalletHolder
import nl.tudelft.trustchain.atomicswap.swap.WalletHolder.bitcoinWallet
import nl.tudelft.trustchain.atomicswap.swap.WalletHolder.ethSwap
import nl.tudelft.trustchain.atomicswap.swap.WalletHolder.ethereumWallet
import nl.tudelft.trustchain.atomicswap.swap.eth.AtomicSwapContract
import nl.tudelft.trustchain.atomicswap.swap.eth.EthereumSwap
import nl.tudelft.trustchain.common.ethereum.EthereumWalletService
import nl.tudelft.trustchain.common.ethereum.EthereumWeb3jWallet
import nl.tudelft.trustchain.common.ui.BaseFragment
import org.bitcoinj.wallet.Wallet
import org.bitcoinj.wallet.listeners.WalletChangeEventListener
import org.web3j.tx.RawTransactionManager
import org.web3j.tx.gas.DefaultGasProvider
import org.web3j.utils.Convert
import kotlin.random.Random

class WalletFragment : BaseFragment(R.layout.fragment_atomic_wallet), WalletChangeEventListener {

    private var _binding: FragmentAtomicWalletBinding? = null

    private var _model: WalletViewModel? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!
    private val model get() = _model!!

    private lateinit var clipboard: ClipboardManager


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        clipboard = ContextCompat.getSystemService(
            requireContext(),
            ClipboardManager::class.java
        ) as ClipboardManager


        lifecycleScope.launchWhenStarted {
            launch(Dispatchers.Default) {
                WalletHolder.ethereumWallet = EthereumWalletService.getGlobalWeb3jWallet(requireContext())
                while (true) {
                    delay(4000)
                    try {
                        WalletHolder.ethSwap = EthereumSwap(ethereumWallet.web3j, ethereumWallet.credentials,BuildConfig.ETH_SWAP_CONTRACT,BuildConfig.ETH_CHAIN_ID)
                        break
                    }catch (e: Exception){
                        Log.d("ETHLOG", e.toString())
                    }
                }
            }
        }
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
        model.setBitcoinBalance(WalletHolder.bitcoinWallet.balance.toFriendlyString())

        val bitcoinBalance = binding.bitcoinBalance
        val ethereumBalance = binding.ethereumBalance

        model.bitcoinBalance.observe(viewLifecycleOwner) {
            bitcoinBalance.text = it
        }

        model.ethereumBalance.observe(viewLifecycleOwner) {
            ethereumBalance.text = it
        }

        binding.bitcoinCopyButton.setOnClickListener {
            val address = WalletHolder.bitcoinWallet.currentReceiveAddress().toString()
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

        WalletHolder.bitcoinWallet.addChangeEventListener(this)
        lifecycleScope.launchWhenStarted {
            while (isActive) {
                try {
                    val ether =
                        Convert.fromWei(ethereumWallet.balance().toString(), Convert.Unit.ETHER)
                    model.setEthereumBalance("$ether ETH")
                    delay(1000)
                } catch (_: Exception) {
                    delay(2900)
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        WalletHolder.bitcoinWallet.removeChangeEventListener(this)
        _binding = null
        _model = null
    }

    override fun onWalletChanged(wallet: Wallet) {
        model.setBitcoinBalance(wallet.balance.toFriendlyString())
    }

}
