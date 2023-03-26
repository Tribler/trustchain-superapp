package nl.tudelft.trustchain.detoks

import Wallet
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import nl.tudelft.ipv8.keyvault.PrivateKey
import nl.tudelft.trustchain.common.ui.BaseFragment

// TODO: Rename parameter arguments, choose names that match
// the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"

/**
 * A simple [Fragment] subclass.
 * Use the [WalletFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class WalletFragment : BaseFragment(R.layout.wallet_fragment) {
    private var param1: String? = null
    private var param2: String? = null
    val myPublicKey = getIpv8().myPeer.publicKey
    val wallet = Wallet.getInstance(myPublicKey, getIpv8().myPeer.key as PrivateKey)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            param1 = it.getString(ARG_PARAM1)
            param2 = it.getString(ARG_PARAM2)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.wallet_fragment, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val createCoinButton = view.findViewById<Button>(R.id.button_create_coin)
        createCoinButton.setOnClickListener {
            // Create a new coin and add it to the wallet!
            creatNewCoin(wallet)
        }

        val buttonShow = view.findViewById<Button>(R.id.button_show)
        buttonShow.setOnClickListener {
            createCoinButton.visibility = View.INVISIBLE
            val textBalance = view.findViewById<TextView>(R.id.balance)
            textBalance.text = "Your balance is: " + wallet.balance
        }
    }

    /**
     * Create a new token and add it to the wallet!
     */
    fun creatNewCoin(wallet: Wallet) {
        val token = Token.create(1, myPublicKey.keyToBin())
        wallet.addToken(token)
        println("Wallet A balance: ${wallet.balance}")
    }

    companion object {
        /**
         * Use this factory method to create a new instance of
         * this fragment using the provided parameters.
         *
         * @param param1 Parameter 1.
         * @param param2 Parameter 2.
         * @return A new instance of fragment AddFriendFragment.
         */
        // TODO: Rename and change types and number of parameters
        @JvmStatic
        fun newInstance(param1: String, param2: String) =
            WalletFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_PARAM1, param1)
                    putString(ARG_PARAM2, param2)
                }
            }
    }
}
