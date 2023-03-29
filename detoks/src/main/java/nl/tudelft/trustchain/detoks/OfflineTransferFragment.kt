package nl.tudelft.trustchain.detoks

import Wallet
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.*
import androidx.annotation.RequiresApi
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.findNavController
import kotlinx.android.synthetic.main.fragment_offline_transfer.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import nl.tudelft.ipv8.keyvault.PrivateKey
import nl.tudelft.trustchain.common.ui.BaseFragment
import nl.tudelft.trustchain.common.util.QRCodeUtils
import nl.tudelft.trustchain.detoks.db.DbHelper
import org.json.JSONObject
import java.util.*
import kotlin.collections.ArrayList

// TODO: Rename parameter arguments, choose names that match
// the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"

/**
 * A simple [Fragment] subclass.
 * Use the [OfflineTransferFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class OfflineTransferFragment : BaseFragment(R.layout.fragment_offline_transfer) {
    // TODO: Rename and change types of parameters
    private var param1: String? = null
    private var param2: String? = null

    private val qrCodeUtils by lazy {
        QRCodeUtils(requireContext())
    }

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
        return inflater.inflate(R.layout.fragment_offline_transfer, container, false)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val wallet = Wallet.getInstance(view.context,getIpv8().myPeer.publicKey, getIpv8().myPeer.key as PrivateKey)
        val dbHelper = DbHelper(view.context)
        val friendList = wallet.listOfFriends
        val friendUsernames = arrayListOf<String>()
        for (f in friendList){
            friendUsernames.add(f.username)
        }
        val friends = friendUsernames.toMutableList() // Mutable?
        if (friendUsernames.isEmpty()) {
            friends.add("Add Friend")
        }

//        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, friendList)
//        dropdownMenu.adapter = adapter
//        val friends = Arrays.asList("Vyshnavi", "Ali", "Dany", "Julio")
        val spinnerFriends: Spinner = view.findViewById(R.id.spinner)
        // create an array adapter and pass the required parameter
        // in our case pass the context, drop down layout, and array.
        val arrayAdapter = ArrayAdapter(view.context, R.layout.dropdown_friends, friends)
        // set adapter to the spinner
        spinnerFriends.adapter = arrayAdapter

        val buttonScan = view.findViewById<Button>(R.id.button_send)
        buttonScan.setOnClickListener {
            qrCodeUtils.startQRScanner(this, null, true)
        }

        val buttonAddFriend = view.findViewById<Button>(R.id.button_friend)
        buttonAddFriend.setOnClickListener {
            val navController = view.findNavController()
            navController.navigate(R.id.addFriendFragment)
        }

        val buttonRequest = view.findViewById<Button>(R.id.button_request)
        val amountText = view.findViewById<EditText>(R.id.amount)
        val amount = amountText.text


        buttonRequest.setOnClickListener {
            val friendUsername = spinnerFriends.selectedItem
            val friendPublicKey = dbHelper.getFriendsPublicKey(friendUsername.toString())
            // After tranfering the requested amount, remove it from the owner wallet.
            val tokens = wallet.getPayment(amount.toString().toInt())  // removes the tokens from the db
            showQR(view, tokens, friendPublicKey)
        }

//    companion object {
//        /**
//         * Use this factory method to create a new instance of
//         * this fragment using the provided parameters.
//         *
//         * @param param1 Parameter 1.
//         * @param param2 Parameter 2.
//         * @return A new instance of fragment OfflineTransferFragment.
//         */
//        // TODO: Rename and change types and number of parameters
//        @JvmStatic
//        fun newInstance(param1: String, param2: String) =
//            OfflineTransferFragment().apply {
//                arguments = Bundle().apply {
//                    putString(ARG_PARAM1, param1)
//                    putString(ARG_PARAM2, param2)
//                }
//            }
//    }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
//        super.onActivityResult(requestCode, resultCode, data)
        val content = qrCodeUtils.parseActivityResult(requestCode, resultCode, data)
        Log.v("Transfer data ", content.toString())

        if (content != null) {
            // deserialize the content
//            val obtainedTokens = Token.deserialize(content.toByteArray())
            // add tokens to wallet
        } else {
            Toast.makeText(this.context, "Scanning failed!", Toast.LENGTH_LONG).show()
        }
        // retrieve the collection of tokens
        // deserialize it
        // increase the amount in the wallet
    }

    private fun createNextOwner(
        tokens: ArrayList<Token>,
        pubKeyRecipient: ByteArray
    ) : ArrayList<Token> {
        val senderPrivateKey = getIpv8().myPeer.key

        // create the new ownership of the token
        for(token in tokens) {
            token.signByPeer(pubKeyRecipient, senderPrivateKey as PrivateKey)
        }

        return tokens
    }


    private fun showQR(view: View, token: ArrayList<Token>, friendPublicKey: ByteArray) {
        val newToken = createNextOwner(token, friendPublicKey)
        // encode newToken

        val jsonObject = JSONObject()
        jsonObject.put("token", newToken)
//        val amountText = view.findViewById<EditText>(R.id.amount)
//        val amount = amountText.text
//        jsonObject.put("amount_requested", amount)
        val jsonString = jsonObject.toString()
        hideKeyboard()
        lifecycleScope.launch {
            val bitmap = withContext(Dispatchers.Default) {
                qrCodeUtils.createQR(jsonString)
            }
            val qrCodeImage = view.findViewById<ImageView>(R.id.QR)
            qrCodeImage.setImageBitmap(bitmap)
        }
//        amountText.text.clear()
//        amountText.clearFocus()
        button_send.visibility = View.INVISIBLE
    }

    private fun hideKeyboard() {
        val inputManager = activity?.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        if (inputManager.isAcceptingText) {
            inputManager.hideSoftInputFromWindow(activity?.currentFocus?.windowToken, 0)
        }
    }
}
