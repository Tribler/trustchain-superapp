package nl.tudelft.trustchain.detoks

import Wallet
import android.content.res.ColorStateList
import android.os.Build
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.annotation.RequiresApi
import androidx.lifecycle.LiveData
import androidx.lifecycle.liveData
import androidx.navigation.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.mattskala.itemadapter.Item
import com.mattskala.itemadapter.ItemAdapter
import nl.tudelft.ipv8.keyvault.PrivateKey
import nl.tudelft.trustchain.common.ui.BaseFragment
import java.time.LocalDateTime

// TODO: Rename parameter arguments, choose names that match
// the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"

/**
 * A simple [Fragment] subclass.
 * Use the [WalletFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class WalletFragment : BaseFragment(R.layout.wallet_fragment), TokenButtonListener {
    private var param1: String? = null
    private var param2: String? = null
    val myPublicKey = getIpv8().myPeer.publicKey


    private val adapter = ItemAdapter()

    private val items: LiveData<List<Item>> by lazy {
        liveData { emit(listOf<Item>()) }
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        adapter.registerRenderer(TokenAdminItemRenderer("user", this))
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

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val wallet = Wallet.getInstance(view.context, myPublicKey, getIpv8().myPeer.key as PrivateKey)
        val createCoinButton = view.findViewById<Button>(R.id.button_create_coin)

        val recyclerView: RecyclerView = view.findViewById(R.id.listView)

        // Set Balance
        val balanceText = view.findViewById<TextView>(R.id.balance)
        balanceText.text = wallet.balance.toString()

        var tokenList = getCurrentCoins(wallet.getTokens()).map { token: Token -> TokenItem(token) }
        updateTokenList(tokenList, recyclerView, view)

        createCoinButton.setOnClickListener {
            // Create a new coin and add it to the wallet!
            creatNewCoin(wallet)
            balanceText.text = wallet.balance.toString()
        }

//        val buttonShow = view.findViewById<Button>(R.id.button_show)
//        buttonShow.setOnClickListener {
//            createCoinButton.visibility = View.INVISIBLE
//            val textBalance = view.findViewById<TextView>(R.id.balance)
//            textBalance.text = "Your balance is: " + wallet.balance
//        }

        val buttonAdminPage = view.findViewById<Button>(R.id.button_admin_page)
        buttonAdminPage.setOnClickListener {
            val navController = view.findNavController()
            navController.navigate(R.id.adminFragment)
        }

        val expiredTokens = view.findViewById<Button>(R.id.expiredCoins)
        val buttonTokenList = view.findViewById<Button>(R.id.button_show)

        buttonTokenList.setOnClickListener {
            buttonTokenList.backgroundTintList = ColorStateList.valueOf(resources.getColor(R.color.gray,
                context?.theme
            ));
            buttonTokenList.setTextColor(resources.getColor(R.color.white, context?.theme));
            expiredTokens.backgroundTintList = ColorStateList.valueOf(resources.getColor(R.color.light_gray, context?.theme));
            expiredTokens.setTextColor(resources.getColor(R.color.black, context?.theme));

            // Update Recycler View with current tokens
            val currentTokens = getCurrentCoins(wallet.getTokens())

            tokenList = currentTokens.map { token: Token -> TokenItem(token) }
            updateTokenList(tokenList, recyclerView, view)

//            val navController = view.findNavController()
//            val bundle = Bundle().apply {
//                putString("access", "user")
//            }
//            val items = wallet.getTokens().map { token: Token -> TokenItem(token) }
//            navController.navigate(R.id.tokenListAdmin, bundle)
        }

        expiredTokens.setOnClickListener {
            expiredTokens.backgroundTintList = ColorStateList.valueOf(resources.getColor(R.color.gray,
                context?.theme
            ));
            expiredTokens.setTextColor(resources.getColor(R.color.white, context?.theme));
            buttonTokenList.backgroundTintList = ColorStateList.valueOf(resources.getColor(R.color.light_gray, context?.theme));
            buttonTokenList.setTextColor(resources.getColor(R.color.black, context?.theme));

            val expiredTokensList = getExpiredCoins(wallet.getTokens())

            tokenList = expiredTokensList.map { token: Token -> TokenItem(token) }
            updateTokenList(tokenList, recyclerView, view)

//            val navController = view.findNavController()
//            val bundle = Bundle().apply {
//                putString("access", "user")
//            }
//            val items = wallet.getTokens().map { token: Token -> TokenItem(token) }
//            navController.navigate(R.id.tokenListAdmin, bundle)
        }

    }

    private fun updateTokenList(
        tokenList: List<TokenItem>,
        recyclerView: RecyclerView,
        view: View
    ) {
        adapter.updateItems(tokenList)
        recyclerView.layoutManager = LinearLayoutManager(view.context)
        recyclerView.adapter = adapter
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun getCurrentCoins(tokens: MutableList<Token>): MutableList<Token> {
        val currentTokens = mutableListOf<Token>()
        for (i in tokens) {
            if (LocalDateTime.now().minute - i.timestamp.minute < 3) {
                print("Current: " + LocalDateTime.now().minute.toString() + " , Token: " + i.timestamp.minute.toString())
                currentTokens.add(i)
            }
        }
        return currentTokens
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun getExpiredCoins(tokens: MutableList<Token>): MutableList<Token> {
        val expiredTokens = mutableListOf<Token>()
        for (i in tokens) {
            if (LocalDateTime.now().minute - i.timestamp.minute >= 3) {
                print("Current: " + LocalDateTime.now().minute.toString() + " , Token: " + i.timestamp.minute.toString())
                expiredTokens.add(i)
            }
        }
        return expiredTokens
    }
    /**
     * Create a new token and add it to the wallet!
     */
    @RequiresApi(Build.VERSION_CODES.O)
    fun creatNewCoin(wallet: Wallet) {
        val myPrivateKey = getIpv8().myPeer.key as PrivateKey
        val token = Token.create(1, myPublicKey.keyToBin())
        val proof = myPrivateKey.sign(token.id + token.value + token.genesisHash + myPublicKey.keyToBin())
        token.recipients.add(RecipientPair(myPublicKey.keyToBin(), proof))
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
        return
    }
}
