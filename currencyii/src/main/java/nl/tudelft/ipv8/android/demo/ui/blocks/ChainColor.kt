package nl.tudelft.ipv8.android.demo.ui.blocks

import android.content.Context
import androidx.core.content.res.ResourcesCompat
import nl.tudelft.ipv8.android.demo.R
import kotlin.math.abs

object ChainColor {
    /**
     * Get color based on hash.
     */
    fun getColor(context: Context, hash: String): Int {
        val colors = context.resources.getIntArray(R.array.colorsChain)
        val number = abs(hash.hashCode() % colors.size)
        return colors[number]
    }

    /**
     * Get the color of the current app user.
     */
    fun getMyColor(context: Context): Int {
        return ResourcesCompat.getColor(context.resources, R.color.colorPrimary, null)
    }
}
