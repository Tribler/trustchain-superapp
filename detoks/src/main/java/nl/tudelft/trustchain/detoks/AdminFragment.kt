package nl.tudelft.trustchain.detoks

import AdminWallet
import Wallet
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.annotation.RequiresApi
import androidx.navigation.findNavController
import nl.tudelft.ipv8.keyvault.PrivateKey
import nl.tudelft.trustchain.common.ui.BaseFragment

class AdminFragment : BaseFragment(R.layout.fragment_admin) {

    private val myPublicKey = getIpv8().myPeer.publicKey

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_admin, container, false)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val wallet = AdminWallet.getInstance(view.context)
        val userWallet = Wallet.getInstance(view.context, myPublicKey, getIpv8().myPeer.key as PrivateKey)

        val createTokenButton = view.findViewById<Button>(R.id.buttonTokenCreate)
        createTokenButton.setOnClickListener {
            // Create a new coin and add it to the wallet!
            creatNewCoin(wallet, userWallet)
        }

        val buttonTokenList = view.findViewById<Button>(R.id.buttonTokenView)
        buttonTokenList.setOnClickListener {
            val navController = view.findNavController()
            val bundle = Bundle().apply {
                putString("access", "admin")
            }
            navController.navigate(R.id.tokenListAdmin, bundle)
        }

    }

    @RequiresApi(Build.VERSION_CODES.O)
    fun creatNewCoin(wallet: AdminWallet, userWallet: Wallet) {
        val myPrivateKey = getIpv8().myPeer.key as PrivateKey
        val token = Token.create(1, myPublicKey.keyToBin())
        val proof = myPrivateKey.sign(token.id + token.value + token.genesisHash + myPublicKey.keyToBin())
        token.recipients.add(RecipientPair(myPublicKey.keyToBin(), proof))

        wallet.addToken(token)
        userWallet.addToken(token)

        println("Wallet A balance: ${wallet.balance}")
        println("Wallet B balance: ${userWallet.balance}")
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
