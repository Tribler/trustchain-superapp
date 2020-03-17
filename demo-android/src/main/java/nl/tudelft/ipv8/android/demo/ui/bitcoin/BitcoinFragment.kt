package nl.tudelft.ipv8.android.demo.ui.bitcoin

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.google.common.util.concurrent.Service.State.RUNNING
import kotlinx.android.synthetic.main.fragment_bitcoin.*
import nl.tudelft.ipv8.android.demo.R
import nl.tudelft.ipv8.android.demo.coin.BitcoinNetworkOptions
import nl.tudelft.ipv8.android.demo.coin.SerializedDeterminsticKey
import nl.tudelft.ipv8.android.demo.coin.WalletManagerAndroid
import nl.tudelft.ipv8.android.demo.coin.WalletManagerConfiguration
import nl.tudelft.ipv8.android.demo.ui.BaseFragment

/**
 * A simple [Fragment] subclass.
 * Use the [BitcoinFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class BitcoinFragment(
    override val controller: BitcoinViewController
) : BitcoinView, BaseFragment(R.layout.fragment_bitcoin) {

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        initClickListeners()
    }

    private fun initClickListeners() {
        show_wallet_button.setOnClickListener {
            controller.showView("MySharedWalletsFragment")
        }

        create_wallet_button.setOnClickListener {
            controller.showView("CreateSWFragment")
        }

        search_wallet_button.setOnClickListener {
            controller.showView("JoinNetworkFragment")
        }

        startWalletButtonExisting.setOnClickListener {
            val config = WalletManagerConfiguration(
                BitcoinNetworkOptions.TEST_NET
            )
            WalletManagerAndroid.Factory(this.requireContext().applicationContext)
                .setConfiguration(config)
                .init()
        }

        startWalletButtonImportDefaultKey.setOnClickListener {
            val config = WalletManagerConfiguration(
                BitcoinNetworkOptions.TEST_NET,
                SerializedDeterminsticKey(
                    "spell seat genius horn argue family steel buyer spawn chef guard vast",
                    1583488954L
                )
            )
            WalletManagerAndroid.Factory(this.requireContext().applicationContext)
                .setConfiguration(config)
                .init()
        }

        refreshButton.setOnClickListener {
            val walletManager = WalletManagerAndroid.getInstance()
            walletStatus.text = "Status: ${walletManager.kit.state()}"
            walletBalance.text =
                "Bitcoin available: ${walletManager.kit.wallet().balance.toFriendlyString()}"
            chosenNetwork.text = "Network: ${walletManager.params.id}"

            if (walletManager.kit.state().equals(RUNNING)) {
                startWalletButtonExisting.isEnabled = false
                startWalletButtonExisting.isClickable = false
                startWalletButtonImportDefaultKey.isEnabled = false
                startWalletButtonImportDefaultKey.isClickable = false
            }

            refresh()
        }

    }

    private fun refresh() {
        val walletManager = WalletManagerAndroid.getInstance()


        Log.i("Coin", "Coin: ${walletManager.toSeed()}")
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_bitcoin, container, false)
    }

    companion object {
        /**
         * Use this factory method to create a new instance of
         * this fragment using the provided parameters.
         *
         * @return A new instance of fragment bitcoinFragment.
         */
        @JvmStatic
        fun newInstance(controller: BitcoinViewController) = BitcoinFragment(controller)
    }
}
