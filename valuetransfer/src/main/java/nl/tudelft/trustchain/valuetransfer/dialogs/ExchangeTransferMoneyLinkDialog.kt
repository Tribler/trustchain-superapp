package nl.tudelft.trustchain.valuetransfer.dialogs

import android.content.Intent
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.*
import androidx.annotation.RequiresApi
import androidx.core.widget.doAfterTextChanged
import androidx.lifecycle.Observer
import com.android.volley.Request
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import kotlinx.android.synthetic.main.dialog_exchange_transfer_link.*
import nl.tudelft.ipv8.android.IPv8Android
import nl.tudelft.ipv8.util.toHex
import nl.tudelft.trustchain.common.BuildConfig
import nl.tudelft.trustchain.common.contacts.ContactStore
import nl.tudelft.trustchain.common.eurotoken.GatewayStore
import nl.tudelft.trustchain.common.eurotoken.TransactionRepository
import nl.tudelft.trustchain.valuetransfer.R
import nl.tudelft.trustchain.valuetransfer.ui.VTDialogFragment
import nl.tudelft.trustchain.valuetransfer.util.*
import java.security.interfaces.RSAPublicKey
import org.json.JSONObject

class ExchangeTransferMoneyLinkDialog(
    private val amount: String?,
    private val isTransfer: Boolean,
    private val message: String? = ""
) : VTDialogFragment() {
    private var transactionAmount = 0L
    private var transactionAmountText = ""
    private var transactionMessage = message ?: " "
    private var isEuroTransfer = false
    private var IBAN = ""
    private lateinit var messageView: EditText
    private lateinit var typeAdapter: ArrayAdapter<CharSequence>
    private val gatewayStoreLink by lazy {
        GatewayStore.getInstance(requireContext())
    }
    protected val transactionRepositoryLink by lazy {
        TransactionRepository(IPv8Android.getInstance().getOverlay()!!, gatewayStoreLink)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreateDialog(savedInstanceState: Bundle?): BottomSheetDialog {
        return activity?.let {
            val bottomSheetDialog = BottomSheetDialog(requireContext(), R.style.BaseBottomSheetDialog)
            val view = layoutInflater.inflate(R.layout.dialog_exchange_transfer_link, null)

            // Fix keyboard exposing over content of dialog
            bottomSheetDialog.behavior.apply {
                skipCollapsed = true
                state = BottomSheetBehavior.STATE_EXPANDED
            }

            setNavigationBarColor(requireContext(), parentActivity, bottomSheetDialog)

            val typeSpinner = view.findViewById<Spinner>(R.id.spinnerType)
            val ibanView = view.findViewById<TextView>(R.id.etIBAN)
            val transactionAmountView = view.findViewById<EditText>(R.id.etTransactionAmount)
            messageView = view.findViewById(R.id.etTransactionMessage)

            if (isTransfer && message != "") {
                messageView.setText(resources.getString(R.string.text_re, transactionMessage))
            }

            transactionAmountView.addDecimalLimiter()

            if (amount != null) {
                transactionAmount = amount.toLong()
                transactionAmountView.setText(formatAmount(amount).toString())
            }

            typeAdapter = ArrayAdapter.createFromResource(
                requireContext(),
                R.array.exchange_types,
                android.R.layout.simple_spinner_item
            )

            typeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_item)
            typeSpinner.adapter = typeAdapter
            typeSpinner.onItemSelectedListener =
                object : AdapterView.OnItemSelectedListener {
                    override fun onItemSelected(
                        parent: AdapterView<*>,
                        view: View?,
                        pos: Int,
                        id: Long
                    ) {
                        ibanView.visibility = if (pos == 0) View.GONE else View.VISIBLE
                        isEuroTransfer = pos != 0
                    }

                    override fun onNothingSelected(p0: AdapterView<*>?) {}
                }

            parentActivity.getBalance(true).observe(
                this,
                Observer {
                    transactionAmountView.hint =
                        if (isTransfer) resources.getString(R.string.text_balance_max, it) else ""
                }
            )

            onFocusChange(transactionAmountView, requireContext())
            onFocusChange(messageView, requireContext())

            transactionAmountView.doAfterTextChanged {
                transactionAmount = formatAmount(transactionAmountView.text.toString())
                transactionAmountText = transactionAmountView.text.toString()
                //                  Large number warning
                if (isValidTransactionAmount(transactionAmountText) == "Valid but large") {
                    transactionAmountView.background.setTint(Color.parseColor("#FFF9BA"))
                }
//                  Invalid:
                else if (isValidTransactionAmount(transactionAmountText) == "Invalid") {
                    transactionAmountView.background.setTint(Color.parseColor("#FFBABA"))
                    parentActivity.displayToast(
                        requireContext(),
                        resources.getString(R.string.transer_money_link_valid_transaction_value)
                    )
                }
//              Is valid:
                else {
                    transactionAmountView.background.setTint(Color.parseColor("#C8FFC4"))
                }
            }

            messageView.doAfterTextChanged {
                transactionMessage = messageView.text.toString()
            }

            ibanView.doAfterTextChanged {
                when {
                    !isValidIban(ibanView.text.toString()) -> {
                        ibanView.background.setTint(Color.parseColor("#FFBABA"))
                    }
                    else -> {
                        ibanView.background.setTint(Color.parseColor("#C8FFC4"))
                        IBAN = ibanView.text.toString()
                    }
                }
            }

            bottomSheetDialog.setContentView(view)
            bottomSheetDialog.show()

            bottomSheetDialog.clShareLink.setOnClickListener {
                if (!isValidIban(ibanView.text.toString()) && isEuroTransfer) {
                    parentActivity.displayToast(
                        requireContext(),
                        resources.getString(R.string.transer_money_link_valid_iban)
                    )
                } else if (transactionAmount <= 0) {
                    parentActivity.displayToast(
                        requireContext(),
                        resources.getString(R.string.transer_money_link_valid_amount)
                    )
                } else {
                    createPaymentId(
                        (transactionAmountText.replace(",", ".").toDouble() * 100).toInt(), if (isEuroTransfer) ibanView.text.toString() else null
                    )
                }
            }

            bottomSheetDialog
        } ?: throw IllegalStateException(resources.getString(R.string.text_activity_not_null_requirement))
    }

    @RequiresApi(Build.VERSION_CODES.O)
    internal fun createPaymentId(amount: Int, iban: String? = null) {
        val host = BuildConfig.DEFAULT_GATEWAY_HOST
        val url = "$host/api/exchange/${if (iban == null) "e2t" else "t2e"}/initiate"
        Log.e("test", url)
        val queue = Volley.newRequestQueue(requireContext())
        // Post parameters
        val jsonObject = JSONObject()

        if (iban == null) {
            jsonObject.put("collatoral_cent", amount)
        } else {
            jsonObject.put("token_amount_cent", amount)
            jsonObject.put("destination_iban", iban)
        }
        // Volley post request with parameters
        val request = JsonObjectRequest(
            Request.Method.POST, url, jsonObject,
            { response ->
                val paymentId = response.getString("payment_id")
                if (iban == null) {
                    val gatewaydata = response.getJSONObject("gateway_connection_data")
                    getEuroTokenCommunity().connectToGateway(
                        gatewaydata.getString("public_key"),
                        gatewaydata.getString("ip"),
                        gatewaydata.getString("port").toInt(),
                        paymentId
                    )
                }
                showLink(host, paymentId)
            },
            { error ->
                Log.e("server_err", error.message ?: error.toString())
                parentActivity.displayToast(
                    requireContext(),
                    resources.getString(R.string.snackbar_unexpected_error_occurred)
                )
            }
        )
        // Add the volley post request to the request queue
        queue.add(request)
    }

    @RequiresApi(Build.VERSION_CODES.M)
    private fun showLink(host: String, paymentId: String) {
        val link = getLink(host, paymentId)
        val sendIntent: Intent = Intent().apply {
            action = Intent.ACTION_SEND
            putExtra(
                Intent.EXTRA_TEXT,
                resources.getString(R.string.text_request_euro_1) +
                    transactionAmountText +
                    resources.getString(R.string.text_request_euro_2) +
                    link
            )
            type = "text/plain"
        }
        val shareIntent = Intent.createChooser(sendIntent, null)
        startActivity(shareIntent)
    }

    @RequiresApi(Build.VERSION_CODES.M)
    fun getLink(host: String, paymentId: String): String {
        val ownKey = transactionRepositoryLink.trustChainCommunity.myPeer.publicKey
        val name =
            ContactStore.getInstance(requireContext()).getContactFromPublicKey(ownKey)?.name
        val url = StringBuilder("http://trustchain.tudelft.nl/requestMoney?")
        // TODO: "simpleParams" is just a temporary fix to get the signature based on unencoded params (the same the recipient gets).
        // This could probably be cleaner with utilizing the URI class instead of a StringBuilder.
        var simpleParams = java.lang.StringBuilder()
        var parameters = java.lang.StringBuilder()
        simpleParams.append("amount=").append(transactionAmountText)
        simpleParams.append("&message=").append(transactionMessage)
        simpleParams.append("&host=").append(host)
        simpleParams.append("&paymentId=").append(paymentId)
        parameters.append("amount=").append(SecurityUtil.urlencode(transactionAmountText))
        parameters.append("&message=").append(SecurityUtil.urlencode(transactionMessage))
        parameters.append("&host=").append(SecurityUtil.urlencode(host))
        parameters.append("&paymentId=").append(SecurityUtil.urlencode(paymentId))
        if (name != null) {
            simpleParams.append("&name=").append(name)
            parameters.append("&name=").append(SecurityUtil.urlencode(name))
        }
        if (isEuroTransfer) {
            simpleParams.append("&t2e=").append(true)
            parameters.append("&t2e=").append(SecurityUtil.urlencode("true"))
            simpleParams.append("&port=").append(BuildConfig.DEFAULT_GATEWAY_PORT)
            parameters.append("&port=").append(SecurityUtil.urlencode(BuildConfig.DEFAULT_GATEWAY_PORT.toString()))
        }
        simpleParams.append("&public=").append(ownKey.keyToBin().toHex())
        parameters.append("&public=").append(SecurityUtil.urlencode(ownKey.keyToBin().toHex()))
        val keyPair = SecurityUtil.generateKey()
        val publicKey = keyPair.public
        val privateKey = keyPair.private
        val pkstring = SecurityUtil.serializePK(publicKey as RSAPublicKey)
        val signature = SecurityUtil.sign(simpleParams.toString(), privateKey)
        parameters.append(("&signature=")).append(SecurityUtil.urlencode(signature))
        parameters.append("&key=").append(SecurityUtil.urlencode(pkstring))
        url.append(parameters)
        return url.toString()
    }

    internal fun isValidIban(iban: String): Boolean {
        if (!"^[0-9A-Z]*\$".toRegex().matches(iban)) {
            return false
        }

        val symbols = iban.trim { it <= ' ' }
        if (symbols.length < 15 || symbols.length > 34) {
            return false
        }

        val swapped = symbols.substring(4) + symbols.substring(0, 4)
        @Suppress("DEPRECATION")
        return swapped.toCharArray()
            .map { it.toInt() }
            .fold(0) { previousMod: Int, _char: Int ->
                val value = Integer.parseInt(Character.toString(_char.toChar()), 36)
                val factor = if (value < 10) 10 else 100
                (factor * previousMod + value) % 97
            } == 1
    }

    internal fun isValidTransactionAmount(transactionAmountText: String): String {
        val transactionAmountDouble = transactionAmountText.replace(",", ".").toDoubleOrNull()
        if (transactionAmountDouble != null) {
//          Large number warning
            return if (transactionAmountDouble >= HIGHTRANSACTIONWARNINGVALUE && transactionAmountDouble < TOOHIGHTRANSACTIONVALUE) {
                "Valid but large"
            }
//          Too large:
            else if (transactionAmountDouble >= TOOHIGHTRANSACTIONVALUE) {
                "Invalid"
            }
//          Is valid:
            else {
                "Valid"
            }
//      Not valid:
        } else {
            return "Invalid"
        }
    }

    companion object {
        const val TAG = "exchange_transfer_money_link_dialog"
        const val HIGHTRANSACTIONWARNINGVALUE = 1000.00
        const val TOOHIGHTRANSACTIONVALUE = 1000000000000.00
    }
}
