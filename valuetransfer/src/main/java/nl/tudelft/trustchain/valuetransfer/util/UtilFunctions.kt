package nl.tudelft.trustchain.valuetransfer.util

import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.drawable.Drawable
import android.text.Editable
import android.text.TextWatcher
import android.util.TypedValue
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import androidx.core.content.ContextCompat
import androidx.fragment.app.DialogFragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomsheet.BottomSheetDialog
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import nl.tudelft.trustchain.common.util.QRCodeUtils
import nl.tudelft.trustchain.valuetransfer.R
import nl.tudelft.trustchain.valuetransfer.ValueTransferMainActivity
import org.json.JSONObject
import java.math.RoundingMode
import java.security.MessageDigest
import java.util.*
import kotlin.math.abs

fun View.closeKeyboard(context: Context) {
    val inputMethodManager = context.getSystemService(Activity.INPUT_METHOD_SERVICE) as InputMethodManager
    inputMethodManager.hideSoftInputFromWindow(this.windowToken, 0)
    clearFocus()
}

fun EditText.showKeyboard(context: Context) {
    requestFocus()
    val inputMethodManager = context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
    inputMethodManager.showSoftInput(this, InputMethodManager.SHOW_IMPLICIT)
}

fun onFocusChange(editText: EditText, context: Context) {
    editText.onFocusChangeListener = View.OnFocusChangeListener { v, hasFocus ->
        if (!hasFocus) {
            v.closeKeyboard(context)
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
            layoutManager.scrollToPosition(lastItemPosition)
        }
    }
}

fun copyToClipboard(context: Context, text: String, label: String) {
    val clipboard = ContextCompat.getSystemService(context, ClipboardManager::class.java)
    val clip = ClipData.newPlainText(label, text)
    clipboard?.setPrimaryClip(clip)
}

fun mapToJSON(attributes: Map<String, String?>): JSONObject {
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
    button.alpha = if (state) 1f else 0.5f
    button.isClickable = state
}

fun toggleButton(button: ImageButton, state: Boolean) {
    button.isEnabled = state
    button.alpha = if (state) 1f else 0.5f
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

    for (x in 0 until dimension) {
        for (y in 0 until dimension) {
            if ((hash[if (x < (dimension / 2)) x else 4 - x].toInt() shr y and 1) == 1) {
                identiconBitmap.setPixel(x, y, foregroundColor)
            }
        }
    }

    val density = resources.displayMetrics.density
    val w = (resources.getDimension(R.dimen.identiconWidth) * density).toInt()
    val h = (resources.getDimension(R.dimen.identiconHeight) * density).toInt()

    return Bitmap.createScaledBitmap(identiconBitmap, w, h, false)
}

fun betweenDates(first: Date, second: Date, days: Boolean? = false): Long {
    val higher = if (first.time >= second.time) first else second
    val lower = if (first.time < second.time) first else second

    val dayCount = (higher.time - lower.time) / (24 * 60 * 60 * 1000)

    if (days == true) {
        return dayCount
    }

    return (dayCount / 365)
}

fun getColorIDFromThemeAttribute(parentActivity: ValueTransferMainActivity, color: Int): Int {
    val typedValue = TypedValue()
    parentActivity.theme.resolveAttribute(color, typedValue, true)
    return typedValue.resourceId
}

fun DialogFragment.setNavigationBarColor(
    context: Context,
    parentActivity: ValueTransferMainActivity,
    dialog: BottomSheetDialog
) {
    dialog.window!!.navigationBarColor = ContextCompat.getColor(context, getColorIDFromThemeAttribute(parentActivity, R.attr.colorPrimary))
}

fun Int.dpToPixels(context: Context): Int = TypedValue.applyDimension(
    TypedValue.COMPLEX_UNIT_DIP,
    this.toFloat(),
    context.resources.displayMetrics
).toInt()

fun <R> CoroutineScope.executeAsyncTask(
    onPreExecute: () -> Unit,
    doInBackground: () -> R,
    onPostExecute: (R) -> Unit
) = launch {
    onPreExecute()
    val result = withContext(Dispatchers.IO) {
        doInBackground()
    }
    onPostExecute(result)
}

fun hashString(input: String, algorithm: String): String {
    return MessageDigest
        .getInstance(algorithm)
        .digest(input.toByteArray())
        .fold("", { str, it -> str + "%02x".format(it) })
}

fun String.md5(): String {
    return hashString(this, "MD5")
}

fun hashBytes(input: ByteArray, algorithm: String): String {
    return MessageDigest
        .getInstance(algorithm)
        .digest(input)
        .fold("", { str, it -> str + "%02x".format(it) })
}

fun ByteArray.md5(): String {
    return hashBytes(this, "MD5")
}

fun String.getInitials(): String {
    val initials = StringBuilder()
    this.split(" ").forEach {
        if (it.isNotEmpty()) initials.append("${it[0].toUpperCase()}.")
    }
    return initials.toString()
}

fun getFormattedSize(size: Double): String {
    return when {
        size >= 1E6 -> StringBuilder()
            .append(size.div(1E6).toBigDecimal().setScale(2, RoundingMode.UP).toDouble())
            .append("MB")
        size >= 1E3 -> StringBuilder()
            .append(size.div(1E3).toBigDecimal().setScale(0, RoundingMode.UP))
            .append("KB")
        else -> StringBuilder()
            .append(size.div(1E0).toBigDecimal().setScale(0, RoundingMode.UP))
            .append("B")
    }.toString()
}

class DividerItemDecorator(private val divider: Drawable) : RecyclerView.ItemDecoration() {

    override fun onDrawOver(canvas: Canvas, parent: RecyclerView, state: RecyclerView.State) {
        val dividerLeft = parent.paddingLeft
        val dividerRight = parent.width - parent.paddingRight
        val childCount = parent.childCount
        for (i in 0 until childCount) {
            val child = parent.getChildAt(i)
            val params = child.layoutParams as RecyclerView.LayoutParams
            val dividerTop = child.bottom + params.bottomMargin
            val dividerBottom: Int = dividerTop + divider.intrinsicHeight
            divider.setBounds(dividerLeft, dividerTop, dividerRight, dividerBottom)
            divider.draw(canvas)
        }
    }
}
