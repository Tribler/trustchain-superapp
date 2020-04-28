package nl.tudelft.trustchain.currencyii.ui.bitcoin

import android.os.Bundle
import android.util.Log
import android.view.*
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import kotlinx.android.synthetic.main.fragment_my_daos.*
import nl.tudelft.ipv8.util.toHex
import nl.tudelft.trustchain.currencyii.R
import nl.tudelft.trustchain.currencyii.sharedWallet.SWJoinBlockTransactionData
import nl.tudelft.trustchain.currencyii.ui.BaseFragment

/**
 * A simple [Fragment] subclass.
 * Use the [MyDAOsFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class MyDAOsFragment : BaseFragment(R.layout.fragment_my_daos) {

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        initMyDAOsView()
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_my_daos, container, false)
    }

    private fun initMyDAOsView() {
        create_dao_fab.setOnClickListener {
            Log.i("Coin", "Go to create DAO from My DAOs")
            findNavController().navigate(R.id.createSWFragment)
        }
        val sharedWalletBlocks = getCoinCommunity().fetchLatestJoinedSharedWalletBlocks()
        val publicKey = getTrustChainCommunity().myPeer.publicKey.keyToBin().toHex()
        val adapter =
            SharedWalletListAdapter(this, sharedWalletBlocks, publicKey, "Click to propose transfer")
        my_daos_list_view.adapter = adapter
        my_daos_list_view.setOnItemClickListener { _, view, position, id ->
            val block = sharedWalletBlocks[position]
            val blockData = SWJoinBlockTransactionData(block.transaction).getData()
            findNavController().navigate(
                MyDAOsFragmentDirections.actionMyDAOsFragmentToSharedWalletTransaction(
                    blockData.SW_UNIQUE_ID,
                    blockData.SW_VOTING_THRESHOLD,
                    blockData.SW_ENTRANCE_FEE,
                    blockData.SW_TRUSTCHAIN_PKS.size,
                    block.calculateHash().toHex()
                )
            )
            Log.i("Coin", "Clicked: $view, $position, $id")
        }

        button_create_dao.setOnClickListener {
            Log.i("Coin", "Onboarding go to create DAO")
            findNavController().navigate(R.id.createSWFragment)
        }
        button_join_daos.setOnClickListener {
            Log.i("Coin", "Onboarding go to join DAO")
            findNavController().navigate(R.id.joinNetworkFragment)
        }

        // Hide/show onboard depending on whether the user has joined shared wallets blocks
        handleOnboardingVisibility(sharedWalletBlocks.isEmpty())
    }

    private fun handleOnboardingVisibility(isOnboarding: Boolean) {
        if (isOnboarding) {
            // Show onboarding components
            not_enrolled_text.visibility = View.VISIBLE
            onboarding_buttons.visibility = View.VISIBLE

            // Hide create DAO button
            create_dao_fab.visibility = View.GONE
        } else {
            // Hide onboarding components
            not_enrolled_text.visibility = View.GONE
            onboarding_buttons.visibility = View.GONE

            // Show create DAO button
            create_dao_fab.visibility = View.VISIBLE
        }
    }
}
