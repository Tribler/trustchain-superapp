package nl.tudelft.trustchain.valuetransfer.dialogs

import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.core.view.isVisible
import androidx.core.widget.doAfterTextChanged
import androidx.lifecycle.*
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import nl.tudelft.trustchain.common.contacts.Contact
import nl.tudelft.trustchain.valuetransfer.R
import nl.tudelft.trustchain.valuetransfer.ui.VTDialogFragment
import nl.tudelft.trustchain.valuetransfer.util.*

class ExchangeTransferMoneyLinkDialog(
    private val amount: String?,
    private val isTransfer: Boolean,
    private val message: String? = ""
) : VTDialogFragment() {

    private var transactionAmount = 0L
    private var transactionMessage = message ?: ""
    private lateinit var messageView: EditText

    private lateinit var typeAdapter: ArrayAdapter<CharSequence>

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
//            val selectedTypeView = view.findViewById<TextView>(R.id.tvSelectedType)
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

            parentActivity.getBalance(true).observe(
                this,
                Observer {
                    transactionAmountView.hint = if (isTransfer) resources.getString(R.string.text_balance_max, it) else ""
                }
            )

            onFocusChange(transactionAmountView, requireContext())
            onFocusChange(messageView, requireContext())

            transactionAmountView.doAfterTextChanged {
                transactionAmount = formatAmount(transactionAmountView.text.toString())
            }

            messageView.doAfterTextChanged {
                transactionMessage = messageView.text.toString()
            }

            bottomSheetDialog.setContentView(view)
            bottomSheetDialog.show()

            bottomSheetDialog
        } ?: throw IllegalStateException(resources.getString(R.string.text_activity_not_null_requirement))
    }

    companion object {
        const val TAG = "exchange_transfer_money_link_dialog"
    }
}
