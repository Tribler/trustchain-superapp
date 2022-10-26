package nl.tudelft.trustchain.peerchat.ui.conversation

import android.content.Context
import android.util.AttributeSet
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputConnection
import androidx.appcompat.R
import androidx.core.view.inputmethod.EditorInfoCompat
import androidx.core.view.inputmethod.InputConnectionCompat

class RichEditText @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = R.attr.editTextStyle
) : androidx.appcompat.widget.AppCompatEditText(context, attrs, defStyleAttr) {
    var onCommitContentListener: InputConnectionCompat.OnCommitContentListener? = null

    override fun onCreateInputConnection(editorInfo: EditorInfo): InputConnection {
        val ic: InputConnection = super.onCreateInputConnection(editorInfo)
        EditorInfoCompat.setContentMimeTypes(editorInfo, arrayOf("image/*"))

        val callback =
            InputConnectionCompat.OnCommitContentListener { inputContentInfo, flags, opts ->
                onCommitContentListener?.onCommitContent(inputContentInfo, flags, opts) ?: false
            }

        @Suppress("DEPRECATION")
        return InputConnectionCompat.createWrapper(ic, editorInfo, callback)
    }
}
