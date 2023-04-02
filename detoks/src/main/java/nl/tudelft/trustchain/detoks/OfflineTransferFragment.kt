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
import com.google.common.primitives.UnsignedBytes.toInt
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
    var arrayAdapter: ArrayAdapter<String>? = null
    var wallet : Wallet? = null
    var spinnerFriends: Spinner? = null

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

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View? {
        val view = inflater.inflate(R.layout.fragment_offline_transfer, container, false)
        wallet = Wallet.getInstance(view.context, getIpv8().myPeer.publicKey, getIpv8().myPeer.key as PrivateKey)

        val friends = wallet!!.getListOfFriends()
        val friendUsernames = mutableListOf<String>()
        for (f in friends){
            friendUsernames.add(f.username)
        }

        spinnerFriends = view.findViewById(R.id.spinner)
        // create an array adapter and pass the required parameter
        // in our case pass the context, drop down layout, and array.
        arrayAdapter = ArrayAdapter(view.context, R.layout.dropdown_friends, friendUsernames)
        // set adapter to the spinner
        spinnerFriends?.adapter = arrayAdapter
        arrayAdapter?.notifyDataSetChanged()
        spinnerFriends?.refreshDrawableState()

        // Inflate the layout for this fragment
        return view
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val myPublicKey = getIpv8().myPeer.publicKey
        val myPrivateKey = getIpv8().myPeer.key as PrivateKey
        val amountText = view.findViewById<EditText>(R.id.amount)


        val buttonScan = view.findViewById<Button>(R.id.button_send)
        buttonScan.setOnClickListener {
            qrCodeUtils.startQRScanner(this, null, true)
        }

        val buttonAddFriend = view.findViewById<Button>(R.id.button_friend)
        buttonAddFriend.setOnClickListener {
            val navController = view.findNavController()
            navController.navigate(R.id.addFriendFragment)
        }


        val token = Token.create(1, myPublicKey.keyToBin())
        val proof = myPrivateKey.sign(token.id + token.value + token.genesisHash + myPublicKey.keyToBin())
        token.recipients.add(RecipientPair(myPublicKey.keyToBin(), proof))

//        val result = wallet!!.addToken(token)
//        if (result != -1L) {
            Toast.makeText(this.context, "Balance " + wallet!!.balance.toString(), Toast.LENGTH_LONG).show()
//        } else {
//            Toast.makeText(this.context, "Duplicate token!", Toast.LENGTH_LONG)
//                .show()
//        }

        val buttonRequest = view.findViewById<Button>(R.id.button_request)
        val dbHelper = DbHelper(view.context)
        buttonRequest.setOnClickListener {
            //friend selected
            val friendUsername = spinnerFriends?.selectedItem
            val amount = amountText.text

            //get the friends public key from the db
            val friendPublicKey = dbHelper.getFriendsPublicKey(friendUsername.toString())
            try {
                //if(amount.toString().toInt() >= 0) {
                        if (wallet!!.balance > 0) {
                            val chosenTokens = wallet!!.getPayment(amount.toString().toInt())
                            if(chosenTokens == null){
                                Toast.makeText(this.context, "Not Successful (not enough money or could get amount)", Toast.LENGTH_LONG).show()
                            } else {
                                showQR(view, chosenTokens, friendPublicKey)
                                Toast.makeText(this.context, "Successful " + wallet!!.balance.toString(), Toast.LENGTH_LONG).show()

                            }
                        } else {
                            Toast.makeText(this.context, "No money - balance is 0", Toast.LENGTH_LONG).show()
                        }
                //TODO: disappear text field, button, spinner
                // write some message you are sending blaabla
                // check amount more than 0
            } catch (e : NumberFormatException){
                Toast.makeText(this.context, "Please specify positive amount!", Toast.LENGTH_LONG).show()
            }

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

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
//        super.onActivityResult(requestCode, resultCode, data)
        val content = qrCodeUtils.parseActivityResult(requestCode, resultCode, data)
        Log.v("Transfer data ", content.toString())

        if (content != null) {
            // deserialize the content
            val obtainedTokens = Token.deserialize(content.toByteArray())
            for(t in obtainedTokens)
                wallet!!.addToken(t)
        } else {
            Toast.makeText(this.context, "Scanning failed!", Toast.LENGTH_LONG).show()
        }
    }

    private fun createNextOwner(tokens : ArrayList<Token>, pubKeyRecipient: ByteArray) : ArrayList<Token> {
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
