package nl.tudelft.trustchain.detoks.helpers

import android.view.View
import android.view.View.OnClickListener
import mu.KotlinLogging

/**
 * Custom class to add a listener for double clicking
 */
abstract class DoubleClickListener : OnClickListener {

    private var lastClickTime: Long = 0
    private var doubleClickWindow: Long = 300 // milliseconds

    /**
     * Checks if there was a click registered at most [doubleClickWindow] milliseconds
     * before the current click.
     * @returns True iff there was a click registered in the time window
     */
    private fun checkForDoubleClick(): Boolean {

        val clickTime = System.currentTimeMillis()

        if (clickTime - lastClickTime < doubleClickWindow) {
            return true
        }

        lastClickTime = clickTime

        return false
    }

    override fun onClick(view: View?) {

        if (checkForDoubleClick()) {
            onDoubleClick(view)
        }
    }

    /**
     * Action on double click
     */
    abstract fun onDoubleClick(view: View?);
}
