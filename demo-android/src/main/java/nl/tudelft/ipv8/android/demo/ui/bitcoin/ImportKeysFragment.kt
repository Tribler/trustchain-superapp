package nl.tudelft.ipv8.android.demo.ui.bitcoin

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import kotlinx.android.synthetic.main.fragment_import_keys.*
import nl.tudelft.ipv8.android.demo.R
import nl.tudelft.ipv8.android.demo.coin.*

/**
 * A simple [Fragment] subclass.
 * Use the [ImportKeysFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class ImportKeysFragment : Fragment() {

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        import_btc_address_btn.setOnClickListener {

            val config = WalletManagerConfiguration(
                if (net_switch.isChecked) BitcoinNetworkOptions.TEST_NET else BitcoinNetworkOptions.PRODUCTION,
                null,
                AddressPrivateKeyPair(pk_input.text.toString(), sk_input.text.toString())
            )

            WalletManagerAndroid.Factory(this.requireContext().applicationContext)
                .setConfiguration(config)
                .init()

            pk_input.setText("")
            sk_input.setText("")
            net_switch.isSelected = true

        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_import_keys, container, false)
    }

    companion object {
        /**
         * Use this factory method to create a new instance of
         * this fragment using the provided parameters.
         *
         * @return A new instance of fragment ImportKeysFragment.
         */
        // TODO: Rename and change types and number of parameters
        @JvmStatic
        fun newInstance() = ImportKeysFragment()
    }
}
