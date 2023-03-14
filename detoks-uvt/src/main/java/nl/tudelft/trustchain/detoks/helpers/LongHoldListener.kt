package nl.tudelft.trustchain.detoks.helpers

import android.graphics.Rect
import android.view.MotionEvent
import android.view.View
import android.view.View.OnTouchListener
import android.os.Handler
import android.view.ViewConfiguration

/**
 * Custom class to add a listener for double clicking
 */
@Suppress("DEPRECATION")
abstract class LongHoldListener : OnTouchListener {
    private var minimumHoldTime: Long = 2000

    private val handler: Handler = Handler()
    private var boundaries: Rect? = null
    private var onTap = Runnable {
        handler.postDelayed(onLongPress, minimumHoldTime - ViewConfiguration.getTapTimeout().toLong())
    }
    private var onLongPress = Runnable {
        onLongHold()
    }

    /*
    Source: https://stackoverflow.com/questions/7919865/detecting-a-long-press-in-android
     */
    override fun onTouch(view: View, event: MotionEvent): Boolean {
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                boundaries = Rect(view.left, view.top, view.right, view.bottom)
                handler.postDelayed(onTap, ViewConfiguration.getTapTimeout().toLong())
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                handler.removeCallbacks(onLongPress)
                handler.removeCallbacks(onTap)
            }
            MotionEvent.ACTION_MOVE -> {
                if (!boundaries!!.contains(view.left + event.x.toInt(), view.top + event.y.toInt())) {
                    handler.removeCallbacks(onLongPress)
                    handler.removeCallbacks(onTap)
                }
            }
        }
        return true
    }

    /**
     * Action on long hold
     */
    abstract fun onLongHold();
}
