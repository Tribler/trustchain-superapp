package nl.tudelft.trustchain.valuetransfer.ui.walletoverview

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import nl.tudelft.trustchain.common.ui.BaseFragment
import nl.tudelft.trustchain.common.util.viewBinding
import nl.tudelft.trustchain.valuetransfer.R
import nl.tudelft.trustchain.valuetransfer.ValueTransferMainActivity
import nl.tudelft.trustchain.valuetransfer.databinding.FragmentWalletOverviewBinding
import nl.tudelft.trustchain.valuetransfer.db.IdentityStore

class WalletOverviewFragmentBak : BaseFragment(R.layout.fragment_wallet_overview) {

    private val binding by viewBinding(FragmentWalletOverviewBinding::bind)
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

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_wallet_overview, container, false)
    }

    override fun onResume() {
        super.onResume()
        (requireActivity() as ValueTransferMainActivity).setActionBarTitle("Wallet")
        (requireActivity() as ValueTransferMainActivity).toggleActionBar(true)
    }

//    override fun onResume() {
//        super.onResume()
//
//        (requireActivity() as ValueTransferMainActivity).setActionBarTitle("Wallet")
//        (requireActivity() as ValueTransferMainActivity).toggleActionBar(true)
//    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        identityStore.createAttributesTable()

        onResume()

//        val community = IPv8Android.getInstance().getOverlay<AttestationCommunity>()!!
//        Log.i("PUBLIC KEY:", IPv8Android.getInstance().myPeer.publicKey.toString())

//        getCommunity().testCommunity()

//        Log.d("BALANCE",transactionRepository.getMyVerifiedBalance().toString())

//        getCommunity().deleteDatabase(requireContext())
//        this.context?.deleteDatabase("peerchat.db")
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

//        val bottomNavigationView = requireActivity().findViewById<BottomNavigationView>(R.id.bnvWalletOverview)
//        val navController = requireActivity().findNavController(R.id.navHostFragment)
//        bottomNavigationView.setupWithNavController(navController)

        view.rootView.setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.colorPrimaryValueTransfer))

        (requireActivity() as ValueTransferMainActivity).actionBar?.hide()
        (requireActivity() as ValueTransferMainActivity).setActionBarTitle("Wallet")
        (activity as ValueTransferMainActivity).toggleBottomNavigation(true)

//        if(!identityStore.hasPersonalIdentity()) {
//            findNavController().navigate(nl.tudelft.trustchain.valuetransfer.R.id.action_identityOverview_to_identityView)
//        }

        binding.bnvWalletOverview.isVisible = identityStore.hasPersonalIdentity()
        binding.fcvExchange.isVisible = identityStore.hasPersonalIdentity()
        binding.fcvContacts.isVisible = identityStore.hasPersonalIdentity()

    }



//    companion object {
//        const val ARG_PUBLIC_KEY = "public_key"
//        const val ARG_NAME = "name"
//
//        private const val GROUP_TIME_LIMIT = 60 * 1000
//        private const val PICK_IMAGE = 10
//    }
}
