package nl.tudelft.trustchain.eurotoken.ui.exchange

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import kotlinx.android.synthetic.main.fragment_exchange.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import nl.tudelft.ipv8.util.toHex
import nl.tudelft.trustchain.common.contacts.ContactStore
import nl.tudelft.trustchain.common.eurotoken.Transaction
import nl.tudelft.trustchain.common.eurotoken.TransactionRepository
import nl.tudelft.trustchain.common.util.QRCodeUtils
import nl.tudelft.trustchain.eurotoken.R
import nl.tudelft.trustchain.eurotoken.ui.EurotokenBaseFragment
import nl.tudelft.trustchain.eurotoken.ui.transfer.TransferFragment
import nl.tudelft.trustchain.eurotoken.ui.transactions.TransactionItem
import nl.tudelft.trustchain.eurotoken.ui.transfer.TransferFragment.Companion.addDecimalLimiter
import org.json.JSONException
import org.json.JSONObject


// TODO: Rename parameter arguments, choose names that match
// the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"

/**
 * A simple [Fragment] subclass.
 * Use the [ExchangeFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class ExchangeFragment : EurotokenBaseFragment() {
    private var param1: String? = null
    private var param2: String? = null

    private val qrCodeUtils by lazy {
        QRCodeUtils(requireContext())
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {

        class ConnectionData(json: String) : JSONObject(json) {
            val payment_id = this.optString("payment_id")
            val public_key = this.optString("public_key")
            val ip = this.optString("ip")
            val name = this.optString("name")
            val port = this.optInt("port")
            val amount = this.optLong("amount", -1L)
            val type = this.optString("type")
        }
        qrCodeUtils.parseActivityResult(requestCode, resultCode, data)?.let {
            try {
                val connectionData = ConnectionData(it)
                Toast.makeText(requireContext(), connectionData.ip, Toast.LENGTH_LONG).show()
                if (connectionData.type == "destruction") {
                    val args = Bundle()
                    args.putString(DestroyMoneyFragment.ARG_PUBLIC_KEY, connectionData.public_key)
                    args.putString(DestroyMoneyFragment.ARG_NAME, connectionData.name)
                    args.putLong(DestroyMoneyFragment.ARG_AMOUNT, connectionData.amount)
                    args.putInt(DestroyMoneyFragment.ARG_PORT, connectionData.port)
                    args.putString(DestroyMoneyFragment.ARG_IP, connectionData.ip)
                    args.putString(DestroyMoneyFragment.ARG_PAYMENT_ID, connectionData.payment_id)
                    findNavController().navigate(
                        R.id.action_exchangeFragment_to_destroyMoneyFragment,
                        args
                    )
                } else if (connectionData.type == "creation") {
                    val args = Bundle()
                    args.putString(CreateMoneyFragment.ARG_PUBLIC_KEY, connectionData.public_key)
                    args.putString(CreateMoneyFragment.ARG_NAME, connectionData.name)
                    args.putInt(CreateMoneyFragment.ARG_PORT, connectionData.port)
                    args.putString(CreateMoneyFragment.ARG_IP, connectionData.ip)
                    args.putString(CreateMoneyFragment.ARG_PAYMENT_ID, connectionData.payment_id)
                    findNavController().navigate(
                        R.id.action_exchangeFragment_to_createMoneyFragment,
                        args
                    )
                } else {
                    Toast.makeText(requireContext(), "Invalid QR", Toast.LENGTH_LONG).show()
                }
            } catch (e: JSONException) {
                Toast.makeText(requireContext(), "Scan failed, try again", Toast.LENGTH_LONG).show()
            }
        } ?: Toast.makeText(requireContext(), "Scan failed", Toast.LENGTH_LONG).show()
        return
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            param1 = it.getString(ARG_PARAM1)
            param2 = it.getString(ARG_PARAM2)
        }

        lifecycleScope.launchWhenResumed {
            while (isActive) {

                val ownKey = transactionRepository.trustChainCommunity.myPeer.publicKey
                val ownContact =
                    ContactStore.getInstance(requireContext()).getContactFromPublicKey(ownKey)

                txtBalance.text =
                    TransactionRepository.prettyAmount(transactionRepository.getMyVerifiedBalance())
                if (ownContact?.name != null) {
                    txtOwnName.text = "Your balance (" + ownContact.name + ")"
                }
                delay(1000L)
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        txtOwnPublicKey.text = getTrustChainCommunity().myPeer.publicKey.keyToHash().toHex()
        btnCamera.setOnClickListener {
            qrCodeUtils.startQRScanner(this)
        }

        edtAmount.addDecimalLimiter()

        btnInstaSell.setOnClickListener {
            val amount = TransferFragment.getAmount(edtAmount.text.toString())
            if (amount > 0) {
                val iban = edtIBAN.text.toString()
//                if (Regex("^NL\\d{2}\\s[A-Z]{4}\\s0(\\d\\s?){9,30}\$").matches(iban)) {
                transactionRepository.sendDestroyProposalWithIBAN(iban, amount)
                findNavController().navigate(R.id.action_exchangeFragment_to_transactionsFragment)
//                } else {
//                    Toast.makeText(requireContext(), "Please specify a valid iban", Toast.LENGTH_LONG).show()
//                }
            } else {
                Toast.makeText(
                    requireContext(),
                    "Please specify a positive amount",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_exchange, container, false)
    }

    companion object {
        @JvmStatic
        fun newInstance(param1: String, param2: String) =
            ExchangeFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_PARAM1, param1)
                    putString(ARG_PARAM2, param2)
                }
            }

        fun EditText.decimalLimiter(string: String): String {

            var amount = TransferFragment.getAmount(string)

            if (amount == 0L) {
                return ""
            }

            //val amount = string.replace("[^\\d]", "").toLong()
            return (amount / 100).toString() + "." + (amount % 100).toString().padStart(2, '0')
        }

        fun EditText.addDecimalLimiter() {

            this.addTextChangedListener(object : TextWatcher {

                override fun afterTextChanged(s: Editable?) {
                    val str = this@addDecimalLimiter.text!!.toString()
                    if (str.isEmpty()) return
                    val str2 = decimalLimiter(str)

                    if (str2 != str) {
                        this@addDecimalLimiter.setText(str2)
                        val pos = this@addDecimalLimiter.text!!.length
                        this@addDecimalLimiter.setSelection(pos)
                    }
                }

                override fun beforeTextChanged(
                    s: CharSequence?,
                    start: Int,
                    count: Int,
                    after: Int
                ) {
                }

                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            })
        }
    }
}
