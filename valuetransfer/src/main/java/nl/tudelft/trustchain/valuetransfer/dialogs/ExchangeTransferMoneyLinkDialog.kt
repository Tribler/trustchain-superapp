package nl.tudelft.trustchain.valuetransfer.dialogs

import android.content.*
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.annotation.RequiresApi
import androidx.core.widget.doAfterTextChanged
import androidx.lifecycle.Observer
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import kotlinx.android.synthetic.main.dialog_exchange_transfer_link.*
import nl.tudelft.ipv8.android.IPv8Android
import nl.tudelft.trustchain.common.contacts.ContactStore
import nl.tudelft.trustchain.common.eurotoken.GatewayStore
import nl.tudelft.trustchain.common.eurotoken.TransactionRepository
import nl.tudelft.trustchain.valuetransfer.R
import nl.tudelft.trustchain.valuetransfer.ui.VTDialogFragment
import nl.tudelft.trustchain.valuetransfer.util.*
import java.net.URI
import java.net.URLEncoder
import java.security.interfaces.RSAPublicKey
import kotlin.math.sign


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


    @RequiresApi(Build.VERSION_CODES.M)
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

            typeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_item);
            typeSpinner.adapter = typeAdapter
            typeSpinner.onItemSelectedListener =
                object : AdapterView.OnItemSelectedListener {
                    override fun onItemSelected(parent: AdapterView<*>, view: View?, pos: Int, id: Long) {
                        ibanView.visibility = if (pos == 0) View.GONE else View.VISIBLE
                        isEuroTransfer=pos!=0
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
                transactionAmountText=transactionAmountView.text.toString()
            }

            messageView.doAfterTextChanged {
                transactionMessage = messageView.text.toString()
            }

            ibanView.doAfterTextChanged {
                IBAN=ibanView.text.toString()
            }

            bottomSheetDialog.setContentView(view)
            bottomSheetDialog.show()

            bottomSheetDialog.clShareLink.setOnClickListener {
                val link=getLink()
                val sendIntent: Intent = Intent().apply {
                    action = Intent.ACTION_SEND
                    putExtra(Intent.EXTRA_TEXT,
                        "Would you please pay me €$transactionAmountText  via\n$link"
                    )
                    type = "text/plain"
                }
                val shareIntent = Intent.createChooser(sendIntent, null)
                startActivity(shareIntent)
            }

//            bottomSheetDialog.clCopyLink.setOnClickListener{
//                val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
//                val clip: ClipData = ClipData.newPlainText("link",getLink())
//                clipboard.setPrimaryClip(clip)
//                Toast.makeText(this.context, "Text copied to clipboard", Toast.LENGTH_LONG).show()
//            }


            bottomSheetDialog
        } ?: throw IllegalStateException(resources.getString(R.string.text_activity_not_null_requirement))
    }

    @RequiresApi(Build.VERSION_CODES.M)
    private fun  getLink():String
    {
        val ownKey = transactionRepositoryLink.trustChainCommunity.myPeer.publicKey
        val name =
            ContactStore.getInstance(requireContext()).getContactFromPublicKey(ownKey)?.name
        var url=StringBuilder("http://trustchain.tudelft.nl/requestMoney?")
        //TODO: "simpleParams" is just a temporary fix to get the signature based on unencoded params (the same the recipient gets).
        // This could probably be cleaner with utilizing the URI class instead of a StringBuilder.
        var simpleParams=java.lang.StringBuilder()
        var parameters=java.lang.StringBuilder()
        simpleParams.append("amount=").append(transactionAmountText)
        simpleParams.append("&message=").append(transactionMessage)
        parameters.append("amount=").append(SecurityUtil.urlencode(transactionAmountText))
        parameters.append("&message=").append(SecurityUtil.urlencode(transactionMessage))
        if(name!=null) {
            simpleParams.append("&name=").append(name)
            parameters.append("&name=").append(SecurityUtil.urlencode(name))
        }
        if(isEuroTransfer) {
            simpleParams.append("&IBAN=").append(IBAN)
            parameters.append("&IBAN=").append(SecurityUtil.urlencode(IBAN))
        }
        simpleParams.append("&public=").append(ownKey.toString())
        parameters.append("&public=").append(SecurityUtil.urlencode(ownKey.toString()))
        val keyPair=SecurityUtil.generateKey()
        val publicKey=keyPair.public
        val privateKey=keyPair.private
        //url.append(parameters)
        val pkstring=SecurityUtil.serializePK(publicKey as RSAPublicKey)
        val signature=SecurityUtil.sign(simpleParams.toString(), privateKey)
        parameters.append(("&signature=")).append(SecurityUtil.urlencode(signature))
        parameters.append("&key=").append(SecurityUtil.urlencode(pkstring))
        url.append(parameters)
        return url.toString()
    }

    companion object {
        const val TAG = "exchange_transfer_money_link_dialog"
    }
}
