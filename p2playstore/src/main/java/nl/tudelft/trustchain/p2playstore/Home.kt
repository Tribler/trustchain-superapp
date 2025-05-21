package nl.tudelft.trustchain.p2playstore

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import nl.tudelft.ipv8.IPv8
import nl.tudelft.ipv8.android.IPv8Android
import nl.tudelft.ipv8.attestation.trustchain.TrustChainCommunity
import nl.tudelft.trustchain.currencyii.TrustChainHelper
import nl.tudelft.trustchain.p2playstore.databinding.FragmentHomeBinding

class Home : Fragment() {
    protected fun getIpv8(): IPv8 {
        return IPv8Android.getInstance()
    }

    protected fun getP2pStoreCommunity(): P2pStoreCommunity {
        return getIpv8().getOverlay()
            ?: throw IllegalStateException("P2pStoreCommunity is not configured")
    }

    protected val community: P2pStoreCommunity by lazy {
        getP2pStoreCommunity()
    }

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        super.onCreateView(inflater, container, savedInstanceState);
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val wallets = this.community.discoverSharedWallets();
        println("====================================")
        println("wallets: $wallets.length")
        for (wallet in wallets) {
            println(" - wallet: $wallet ${wallet.blockId}")
        }
        println("====================================")

        if (wallets.isEmpty()) {
            println("No wallets found creating one now..")
            try {
                this.community.createBitcoinGenesisWallet(
                    540, 1, this.requireContext()
                )
            }
            catch (e: Exception) {
                println("Failed to create wallet, do you have sufficient funds?\n $e")
            }
        }

        binding.continueButton.setOnClickListener {
            findNavController().navigate(R.id.action_homeFragment_to_myDaosFragment)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
