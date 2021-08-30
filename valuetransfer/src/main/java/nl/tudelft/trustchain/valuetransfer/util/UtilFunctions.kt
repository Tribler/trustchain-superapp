package nl.tudelft.trustchain.valuetransfer.util

import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.Color
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import nl.tudelft.trustchain.common.util.QRCodeUtils
import nl.tudelft.trustchain.valuetransfer.R
import org.json.JSONObject
import java.util.*
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

fun copyToClipboard(context: Context, text: String, label: String) {
    val clipboard = ContextCompat.getSystemService(context, ClipboardManager::class.java)
    val clip = ClipData.newPlainText(label, text)
    clipboard?.setPrimaryClip(clip)
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

fun formatAmount(amount: String): Long {
    val regex = """[^\d]""".toRegex()
    if (amount.isEmpty()) {
        return 0L
    }
    return regex.replace(amount, "").toLong()
}

fun getAmount(amount: String): Long {
    val regex = """[^\d]""".toRegex()
    if (amount.isEmpty()) {
        return 0L
    }
    return regex.replace(amount, "").toLong()
}

fun EditText.decimalLimiter(string: String): String {

    var amount = getAmount(string)
    if (amount == 0L) {
        return "0,00"
    }

    return (amount / 100).toString() + "," + (amount % 100).toString().padStart(2, '0')
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

/**
 * Identicon generation in Github style avatar. The identicon and its colors is composed of the public key. The dimension must be an odd integer of at least 3.
 */
fun generateIdenticon(hash: ByteArray, color: Int, resources: Resources, dimension: Int = 5): Bitmap {

    val foregroundColor = Color.argb(100, Color.red(color), Color.green(color), Color.blue(color))
    var identiconBitmap = Bitmap.createBitmap(dimension, dimension, Bitmap.Config.ARGB_8888)

    for(x in 0 until dimension) {
        for(y in 0 until dimension) {
            if((hash[if(x < (dimension/2)) x else 4 - x].toInt() shr y and 1) == 1) {
                identiconBitmap.setPixel(x, y, foregroundColor)
            }
        }
    }

    val density = resources.displayMetrics.density
    val w = (resources.getDimension(R.dimen.identiconWidth)*density).toInt()
    val h = (resources.getDimension(R.dimen.identiconHeight)*density).toInt()

//    val bitmap = Bitmap.createBitmap(w, h, identiconBitmap.config)
//    val canvas = Canvas(bitmap)
//    identiconBitmap = Bitmap.createScaledBitmap(identiconBitmap, w, h, false)
//    canvas.drawBitmap(identiconBitmap, 0E1F, 0E1F, null)

    return Bitmap.createScaledBitmap(identiconBitmap, w, h, false)
}

fun betweenDates(first: Date, second: Date, days: Boolean? = false): Long {
    val higher = if(first.time >= second.time) first else second
    val lower = if(first.time < second.time) first else second

    val dayCount = (higher.time - lower.time)/(24*60*60*1000)

    if(days == true) {
        return dayCount
    }

    return (dayCount/365)

}
