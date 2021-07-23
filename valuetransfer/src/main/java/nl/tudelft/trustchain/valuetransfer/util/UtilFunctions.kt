package nl.tudelft.trustchain.valuetransfer.util

import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.graphics.Bitmap
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import nl.tudelft.trustchain.common.util.QRCodeUtils
import nl.tudelft.trustchain.eurotoken.ui.transfer.TransferFragment.Companion.decimalLimiter
import org.json.JSONObject
import kotlin.math.abs

fun closeKeyboard(context: Context, view: View) {
    val inputMethodManager = context.getSystemService(Activity.INPUT_METHOD_SERVICE) as InputMethodManager
    inputMethodManager.hideSoftInputFromWindow(view.windowToken, 0)
    view.clearFocus()
}

fun onFocusChange(editText: EditText, context: Context) {
    editText.onFocusChangeListener = View.OnFocusChangeListener { v, hasFocus ->
        if (!hasFocus) {
            closeKeyboard(context, v)
        }
    }
}

fun scrollToBottom(recyclerView: RecyclerView) {
    val layoutManager = recyclerView.layoutManager as LinearLayoutManager?
    val adapter = recyclerView.adapter
    val lastItemPosition = adapter!!.itemCount - 1
    layoutManager!!.scrollToPositionWithOffset(lastItemPosition, 0)

    recyclerView.post {
        val target = layoutManager.findViewByPosition(lastItemPosition)
        if (target != null) {
            val offset = recyclerView.measuredHeight - target.measuredHeight
            layoutManager.scrollToPositionWithOffset(lastItemPosition, offset)
        }
    }
}

fun getFirstLettersFromString(text: String, maxLength: Int) : String {

    if(text.isEmpty()) {
        return ""
    }

    var result = ""
    val arr = text.split(" ")
    val max = arr.size.coerceAtMost(maxLength) -1

    for(ind in 0..max) {
        result += arr[ind].toCharArray()[0]
    }
    return result
}

fun copyToClipboard(context: Context, text: String, label: String) {
    val clipboard =
        ContextCompat.getSystemService(context, ClipboardManager::class.java)
    val clip = ClipData.newPlainText(label, text)
    clipboard?.setPrimaryClip(clip)
    Toast.makeText(context, "$label copied to clipboard", Toast.LENGTH_SHORT).show()
}

fun mapToJSON(attributes: Map<String, String?>) : JSONObject {
    val data = JSONObject()
    for ((key, value) in attributes) {
        data.put(key, value)
    }
    return data
}

fun createBitmap(context: Context, data: String, pColor: Int, bColor: Int): Bitmap {
    return QRCodeUtils(context).createQR(data, pColor = pColor, bColor = bColor)!!
}

fun toggleButton(button: Button, state: Boolean) {
    button.isEnabled = state
    button.alpha = if(state) 1f else 0.5f
    button.isClickable = state
}

fun formatBalance(amount: Long): String {
    return (amount / 100).toString() + "," + (abs(amount) % 100).toString()
        .padStart(2, '0')
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
