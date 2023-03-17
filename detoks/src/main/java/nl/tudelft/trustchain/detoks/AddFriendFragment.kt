package nl.tudelft.trustchain.detoks

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.Toast
import nl.tudelft.trustchain.common.ui.BaseFragment
import nl.tudelft.trustchain.common.util.QRCodeUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import androidx.lifecycle.lifecycleScope

// TODO: Rename parameter arguments, choose names that match
// the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"

/**
 * A simple [Fragment] subclass.
 * Use the [AddFriendFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class AddFriendFragment : BaseFragment(R.layout.fragment_add_friend) {
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
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_add_friend, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val buttonScan = view.findViewById<Button>(R.id.button_scan_public_key)
        buttonScan.setOnClickListener {
            qrCodeUtils.startQRScanner(this, null, true)
        }

        val My_QR = view.findViewById<ImageView>(R.id.My_QR)

        val buttonShow = view.findViewById<Button>(R.id.button_show)
        buttonShow.setOnClickListener {
            buttonScan.visibility = View.INVISIBLE
            val myPublicKey = getIpv8().myPeer.publicKey.toString()
            lifecycleScope.launch {
                var bitmap = withContext(Dispatchers.Default) {
                    // qrCodeUtils.createQR(payload.serialize().toHex())
                    qrCodeUtils.createQR(myPublicKey)
                }
                My_QR.visibility = View.VISIBLE
                My_QR.setImageBitmap(bitmap)

            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        val content = qrCodeUtils.parseActivityResult(requestCode,resultCode,data)
        Log.v("PublicKey content", content.toString())

        val nameFriend = view?.findViewById<EditText>(R.id.name)
        nameFriend?.visibility = View.VISIBLE

        val buttonSave = view?.findViewById<Button>(R.id.save_friend)
        buttonSave?.visibility = View.VISIBLE

        buttonSave?.setOnClickListener{
            val username = nameFriend?.text
           if(nameFriend?.text == null){
               Toast.makeText(this.context,"Enter friend's name!", Toast.LENGTH_LONG).show()
           } else {
               //save call to db
               Log.v("Save pub key", content.toString())
               Log.v("Name ", username.toString())
           }
        }

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
            AddFriendFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_PARAM1, param1)
                    putString(ARG_PARAM2, param2)
                }
            }
    }
}
