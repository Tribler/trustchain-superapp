package nl.tudelft.trustchain.eurotoken

import android.content.Context
import android.graphics.PorterDuff
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.FrameLayout
import kotlinx.android.synthetic.main.view_avatar.view.*
import nl.tudelft.trustchain.common.util.getColorByHash
import nl.tudelft.trustchain.eurotoken.R

class AvatarView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) :
    FrameLayout(context, attrs, defStyleAttr) {

    init {
        LayoutInflater.from(context).inflate(R.layout.view_avatar, this, true)
    }

    fun setUser(id: String, name: String) {
        val initials = name.split(" ")
            .mapNotNull { it.firstOrNull() }
            .take(2)
            .joinToString("")
        txtInitials.text = initials
        imgAvatar.setColorFilter(getColorByHash(context, id), PorterDuff.Mode.MULTIPLY)
    }
}
