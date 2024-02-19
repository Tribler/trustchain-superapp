package nl.tudelft.trustchain.valuetransfer.util

import android.content.Context
import android.view.GestureDetector
import android.view.GestureDetector.SimpleOnGestureListener
import android.view.MotionEvent
import android.view.View
import android.view.View.OnTouchListener
import kotlin.math.abs

internal open class OnSwipeTouchListener(
    context: Context?
) : OnTouchListener {
    private val gestureDetector: GestureDetector
    var scaleFactor = 1.0f

    override fun onTouch(
        view: View,
        motionEvent: MotionEvent
    ): Boolean {
        if (scaleFactor == 1.0f) {
            gestureDetector.onTouchEvent(motionEvent)
        }
        return false
    }

    private inner class GestureListener : SimpleOnGestureListener() {
        private val swipeThreshold: Int = 100
        private val swipeVelocityThreshold: Int = 100

        override fun onDown(e: MotionEvent): Boolean {
            return true
        }

        override fun onSingleTapUp(e: MotionEvent): Boolean {
            onClick()
            return super.onSingleTapUp(e)
        }

        override fun onDoubleTap(e: MotionEvent): Boolean {
            onDoubleClick()
            return super.onDoubleTap(e)
        }

        override fun onLongPress(e: MotionEvent) {
            onLongClick()
            super.onLongPress(e)
        }

        override fun onFling(
            e1: MotionEvent?,
            e2: MotionEvent,
            velocityX: Float,
            velocityY: Float
        ): Boolean {
            try {
                // TODO: Safe call should be used here.
                val diffY = e2.y - e1!!.y
                val diffX = e2.x - e1.x
                if (abs(diffX) > abs(diffY)) {
                    if (abs(diffX) > swipeThreshold && abs(velocityX) > swipeVelocityThreshold) {
                        if (diffX > 0) {
                            onSwipeRight()
                        } else {
                            onSwipeLeft()
                        }
                    }
                } else {
                    if (abs(diffY) > swipeThreshold && abs(velocityY) > swipeVelocityThreshold) {
                        if (diffY < 0) {
                            onSwipeUp()
                        } else {
                            onSwipeDown()
                        }
                    }
                }
            } catch (exception: Exception) {
                exception.printStackTrace()
            }
            return false
        }
    }

    open fun onSwipeRight() {}

    open fun onSwipeLeft() {}

    open fun onSwipeUp() {}

    open fun onSwipeDown() {}

    open fun onSingleTap() {}

    private fun onClick() {}

    private fun onDoubleClick() {}

    private fun onLongClick() {}

    init {
        gestureDetector = GestureDetector(context, GestureListener())
    }
}
