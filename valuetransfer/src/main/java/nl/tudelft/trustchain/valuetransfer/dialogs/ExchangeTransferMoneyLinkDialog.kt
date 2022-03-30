package nl.tudelft.trustchain.valuetransfer.dialogs

import android.content.*
import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.core.widget.doAfterTextChanged
import androidx.lifecycle.Observer
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import kotlinx.android.synthetic.main.dialog_exchange_transfer_link.*
import nl.tudelft.ipv8.android.IPv8Android
import nl.tudelft.ipv8.util.toHex
import nl.tudelft.trustchain.common.contacts.ContactStore
import nl.tudelft.trustchain.common.eurotoken.GatewayStore
import nl.tudelft.trustchain.common.eurotoken.TransactionRepository
import nl.tudelft.trustchain.valuetransfer.R
import nl.tudelft.trustchain.valuetransfer.ui.VTDialogFragment
import nl.tudelft.trustchain.valuetransfer.util.addDecimalLimiter
import nl.tudelft.trustchain.valuetransfer.util.formatAmount
import nl.tudelft.trustchain.valuetransfer.util.onFocusChange
import nl.tudelft.trustchain.valuetransfer.util.setNavigationBarColor


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
                when {
                    !isValidIban(ibanView.text.toString()) -> {
                        ibanView.background.setTint(Color.parseColor("#FFBABA"))
                    }
                    else -> {
                        ibanView.background.setTint(Color.parseColor("#C8FFC4"))
                        IBAN=ibanView.text.toString()
                    }
                }
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
                if (!isValidIban(ibanView.text.toString()))
                    parentActivity.displayToast(
                        requireContext(),
                        resources.getString(R.string.transer_money_link_valid_iban)

                    )
                val shareIntent = Intent.createChooser(sendIntent, null)
                startActivity(shareIntent)
            }

            bottomSheetDialog
        } ?: throw IllegalStateException(resources.getString(R.string.text_activity_not_null_requirement))
    }

    private fun  getLink():String
    {
        val ownKey = transactionRepositoryLink.trustChainCommunity.myPeer.publicKey
        val name =
            ContactStore.getInstance(requireContext()).getContactFromPublicKey(ownKey)?.name
        val url=StringBuilder("http://trustchain.tudelft.nl/requestMoney?")
        url.append("amount=").append(transactionAmountText)
        url.append("&message=").append(transactionMessage)
        if(name!=null)
            url.append("&name=").append(name)
        if(isEuroTransfer)
            url.append("&IBAN=").append(IBAN)
        url.append("&public=").append(ownKey.keyToBin().toHex())
        return url.toString()
    }


    private fun isValidIban(iban: String): Boolean {
        if (!"^[0-9A-Z]*\$".toRegex().matches(iban)) {
            return false
        }

        val symbols = iban.trim { it <= ' ' }
        if (symbols.length < 15 || symbols.length > 34) {
            return false
        }

        val swapped = symbols.substring(4) + symbols.substring(0, 4)
        return swapped.toCharArray()
            .map { it.toInt() }
            .fold(0) { previousMod: Int, _char: Int ->
                val value = Integer.parseInt(Character.toString(_char.toChar()), 36)
                val factor = if (value < 10) 10 else 100
                (factor * previousMod + value) % 97
            } == 1
    }

    companion object {
        const val TAG = "exchange_transfer_money_link_dialog"
    }
}
