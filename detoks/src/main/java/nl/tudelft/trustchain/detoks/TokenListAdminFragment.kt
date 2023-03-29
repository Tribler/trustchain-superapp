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
import androidx.lifecycle.LiveData
import androidx.lifecycle.liveData
import androidx.navigation.findNavController
import com.mattskala.itemadapter.Item
import com.mattskala.itemadapter.ItemAdapter
import nl.tudelft.ipv8.keyvault.PrivateKey
import nl.tudelft.trustchain.common.ui.BaseFragment

class TokenListAdminFragment : BaseFragment(R.layout.fragment_token_list_admin), TokenButtonListener  {

    private val adapter = ItemAdapter()
    private val myPublicKey = getIpv8().myPeer.publicKey

    private var adminWallet: AdminWallet? = null;
    private var userWallet: Wallet? = null;

    private val items: LiveData<List<Item>> by lazy {
        liveData { emit(listOf<Item>()) }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        val user = "admin"

        adapter.registerRenderer(TokenAdminItemRenderer(user, this))

        return inflater.inflate(R.layout.fragment_token_list_admin, container, false)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        adminWallet = AdminWallet.getInstance(view.context)
        userWallet = Wallet.getInstance(view.context, myPublicKey, getIpv8().myPeer.key as PrivateKey)

    }

    override fun onHistoryClick(token: Token, user: String) {
        TODO("Not yet implemented")
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onVerifyClick(token: Token, user: String) {
        val verified = verify(token, user)
        if (verified) {
            reissueToken(token, user)
        }
    }

    fun verify(@Suppress("UNUSED_PARAMETER") token: Token, user: String): Boolean {
        if (user != "admin") {
            return false
        }
        return true
    }

    @RequiresApi(Build.VERSION_CODES.O)
    fun reissueToken(@Suppress("UNUSED_PARAMETER") token: Token, user: String) {
        if (user != "admin") {
            return
        }

        val reissuedToken = token.reissue()

        adminWallet?.addToken(reissuedToken)

        userWallet?.removeToken(token)
        userWallet?.addToken(reissuedToken)
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
