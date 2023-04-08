package nl.tudelft.trustchain.detoks

import AdminWallet
import Wallet
import android.annotation.SuppressLint
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.annotation.RequiresApi
import androidx.lifecycle.LiveData
import androidx.lifecycle.Observer
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.liveData
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.mattskala.itemadapter.Item
import com.mattskala.itemadapter.ItemAdapter
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import nl.tudelft.ipv8.keyvault.PrivateKey
import nl.tudelft.ipv8.util.toHex
import nl.tudelft.trustchain.common.contacts.ContactStore
import nl.tudelft.trustchain.common.eurotoken.Transaction
import nl.tudelft.trustchain.common.eurotoken.TransactionRepository
import nl.tudelft.trustchain.common.ui.BaseFragment
import nl.tudelft.trustchain.common.util.viewBinding
import nl.tudelft.trustchain.detoks.databinding.FragmentTokenListBinding

class TokenListFragment : BaseFragment(R.layout.fragment_token_list), TokenButtonListener  {

    private val myPublicKey = getIpv8().myPeer.publicKey

    private var adminWallet: AdminWallet? = null;
    private var userWallet: Wallet? = null;

    private val binding by viewBinding(FragmentTokenListBinding::bind)

    private val adapter = ItemAdapter()

    private val items: LiveData<List<Item>> by lazy {
        liveData { emit(listOf<Item>()) }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val access = requireArguments().getString("access")
        print("viewInit")
        adapter.registerRenderer(TokenAdminItemRenderer(access!!, this))

        lifecycleScope.launchWhenResumed {
            while (isActive) {
                if (userWallet != null && adminWallet != null) {
                    print("iteration X")
                    if (access == "admin") {
                        val items = adminWallet!!.tokens.map { token: Token -> TokenItem(token) }
                        adapter.updateItems(items)
                    } else if (access == "user") {
                        print("Userbranch")
                        print(userWallet!!.getTokens())
                        val items = userWallet!!.getTokens().map { token: Token -> TokenItem(token) }
                        print(items)
                        adapter.updateItems(items)
                    }

                    adapter.notifyDataSetChanged()
                }
                delay(1000L)
            }
        }

    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_token_list, container, false)
    }

    @RequiresApi(Build.VERSION_CODES.O)
     override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        adminWallet = AdminWallet.getInstance(view.context)
        userWallet = Wallet.getInstance(view.context, myPublicKey, getIpv8().myPeer.key as PrivateKey)

        binding.list.adapter = adapter
        binding.list.layoutManager = LinearLayoutManager(context)
        binding.list.addItemDecoration(DividerItemDecoration(context, LinearLayout.VERTICAL))
//        binding.txtOwnPublicKey.text = getTrustChainCommunity().myPeer.publicKey.keyToHash().toHex()

        items.observe(viewLifecycleOwner, Observer {
            adapter.updateItems(it)
//            binding.txtBalance.text =
//                TransactionRepository.prettyAmount(transactionRepository.getMyVerifiedBalance())
        })
    }

    override fun onHistoryClick(token: Token, access: String) {
        TODO("Not yet implemented")
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onVerifyClick(token: Token, access: String) {
        val verified = verify(token, access)
        if (verified) {
            reissueToken(token, access)
        }
    }

    fun verify(@Suppress("UNUSED_PARAMETER") token: Token, access: String): Boolean {
        if (access != "admin") {
            return false
        }
        return true
    }

    @RequiresApi(Build.VERSION_CODES.O)
    fun reissueToken(@Suppress("UNUSED_PARAMETER") token: Token, access: String) {
        if (access != "admin") {
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
