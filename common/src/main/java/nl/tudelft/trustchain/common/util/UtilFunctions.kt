package nl.tudelft.trustchain.common.util

import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.graphics.Bitmap
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import nl.tudelft.trustchain.common.R
import org.json.JSONObject

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

fun mapToJSON(attributes: Map<String, String>) : JSONObject {
    val data = JSONObject()
    for ((key, value) in attributes) {
        data.put(key, value)
    }
    return data
}

fun createBitmap(context: Context, data: String, pColor: Int, bColor: Int): Bitmap {
    return QRCodeUtils(context).createQR(data, pColor = pColor, bColor = bColor)!!
}

