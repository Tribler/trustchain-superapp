package nl.tudelft.trustchain.valuetransfer.ui.walletoverview

import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.core.content.ContextCompat
import androidx.navigation.findNavController
import androidx.navigation.ui.setupWithNavController
import com.google.android.material.bottomnavigation.BottomNavigationView
import nl.tudelft.ipv8.android.IPv8Android
import com.mattskala.itemadapter.ItemAdapter
import nl.tudelft.ipv8.keyvault.defaultCryptoProvider
import nl.tudelft.ipv8.util.hexToBytes
import nl.tudelft.trustchain.common.eurotoken.GatewayStore
import nl.tudelft.trustchain.common.eurotoken.TransactionRepository
import nl.tudelft.trustchain.common.ui.BaseFragment
import nl.tudelft.trustchain.common.util.viewBinding
import nl.tudelft.trustchain.valuetransfer.R
import nl.tudelft.trustchain.valuetransfer.ValueTransferMainActivity
import nl.tudelft.trustchain.valuetransfer.community.IdentityCommunity
import nl.tudelft.trustchain.valuetransfer.databinding.FragmentWalletOverviewBinding
import nl.tudelft.trustchain.valuetransfer.db.IdentityStore

class WalletOverviewFragment : BaseFragment(R.layout.fragment_wallet_overview) {

//    private val binding by viewBinding(FragmentWalletOverviewBinding::bind)
//
//    private val adapter = ItemAdapter()
//
//    private val store by lazy {
//        IdentityStore.getInstance(requireContext())
//    }

//    private val publicKey by lazy {
//        val publicKeyBin = IPv8Android.getInstance().myPeer.publicKey.toString()
//        defaultCryptoProvider.keyFromPublicBin(publicKeyBin.hexToBytes())
//    }

//    private fun getCommunity() : IdentityCommunity {
//        return getIpv8().getOverlay()
//            ?: throw java.lang.IllegalStateException("IdentityCommunity is not configured")
//    }
    private val identityStore by lazy {
        IdentityStore.getInstance(requireContext())
    }

//    private val gatewayStore by lazy {
//        GatewayStore.getInstance(requireContext())
//    }

//    protected val transactionRepository by lazy {
//        TransactionRepository(getIpv8().getOverlay()!!, gatewayStore)
//    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        identityStore.createAtttributesTable()

//        val community = IPv8Android.getInstance().getOverlay<AttestationCommunity>()!!
//        Log.i("PUBLIC KEY:", IPv8Android.getInstance().myPeer.publicKey.toString())

//        getCommunity().testCommunity()

//        Log.d("BALANCE",transactionRepository.getMyVerifiedBalance().toString())

//        getCommunity().deleteDatabase(requireContext())
//        this.context?.deleteDatabase("peerchat.db")
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val bottomNavigationView = requireActivity().findViewById<BottomNavigationView>(R.id.bnvWalletOverview)
        val navController = requireActivity().findNavController(R.id.navHostFragment)
        bottomNavigationView.setupWithNavController(navController)

        view.rootView.setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.colorPrimaryValueTransfer));

    }



//    companion object {
//        const val ARG_PUBLIC_KEY = "public_key"
//        const val ARG_NAME = "name"
//
//        private const val GROUP_TIME_LIMIT = 60 * 1000
//        private const val PICK_IMAGE = 10
//    }
}
