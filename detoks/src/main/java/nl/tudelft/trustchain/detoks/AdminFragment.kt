package nl.tudelft.trustchain.detoks

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import nl.tudelft.ipv8.keyvault.PrivateKey
import nl.tudelft.trustchain.common.ui.BaseFragment

class AdminFragment : BaseFragment(R.layout.admin_fragment) {

    val myPublicKey = getIpv8().myPeer.publicKey
    val wallet = Wallet.getInstance(myPublicKey, getIpv8().myPeer.key as PrivateKey)
//    val adminWallet =

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.admin_fragment, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val createTokenButton = view.findViewById<Button>(R.id.button_token_create)
        createTokenButton.setOnClickListener {
            // Create a new coin and add it to the wallet!
//            creatNewCoin(wallet)
        }

        val buttonShow = view.findViewById<Button>(R.id.button_token_reissue)
        buttonShow.setOnClickListener {
            // display wallet edited
        }

    }

    companion object {
        /**
         * Use this factory method to create a new instance of
         * this fragment using the provided parameters.
         *
         * @return A new instance of fragment AdminFragment.
         */
        // TODO: Rename and change types and number of parameters
        @JvmStatic
        fun newInstance() =
            WalletFragment().apply {
                arguments = Bundle().apply {
                }
            }
    }

}
