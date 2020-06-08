package nl.tudelft.trustchain.common.util

import android.content.Context
import nl.tudelft.trustchain.common.R
import kotlin.math.abs

/**
 * Get color based on hash.
 */
fun getColorByHash(context: Context, hash: String): Int {
    val colors = context.resources.getIntArray(R.array.colorsChain)
    val number = abs(hash.hashCode() % colors.size)
    return colors[number]
}
