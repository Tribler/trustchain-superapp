package nl.tudelft.trustchain.common.valuetransfer.extensions

import android.content.Context
import android.view.View
import android.view.animation.AnimationUtils
import androidx.core.view.isVisible
import nl.tudelft.trustchain.common.R

fun View.viewEnterFromLeft(
    context: Context,
    duration: Long? = null
) {
    val animation = AnimationUtils.loadAnimation(context, R.anim.enter_from_left)
    if (duration != null) animation.duration = duration

    this.apply {
        isVisible = true
        startAnimation(animation)
    }
}

fun View.viewEnterFromRight(
    context: Context,
    duration: Long? = null
) {
    val animation = AnimationUtils.loadAnimation(context, R.anim.enter_from_right)
    if (duration != null) animation.duration = duration

    this.apply {
        isVisible = true
        startAnimation(animation)
    }
}

fun View.viewExitToLeft(
    context: Context,
    duration: Long? = null
) {
    val animation = AnimationUtils.loadAnimation(context, R.anim.exit_to_left)
    if (duration != null) animation.duration = duration

    this.apply {
        startAnimation(animation)
        isVisible = false
    }
}

fun View.viewExitToRight(
    context: Context,
    duration: Long? = null
) {
    val animation = AnimationUtils.loadAnimation(context, R.anim.exit_to_right)
    if (duration != null) animation.duration = duration

    this.apply {
        startAnimation(animation)
        isVisible = false
    }
}

fun View.viewFadeIn(
    context: Context,
    duration: Long? = null
) {
    val animation = AnimationUtils.loadAnimation(context, R.anim.fade_in)
    if (duration != null) animation.duration = duration

    this.apply {
        isVisible = true
        startAnimation(animation)
    }
}

fun View.viewFadeOut(
    context: Context,
    duration: Long? = null
) {
    val animation = AnimationUtils.loadAnimation(context, R.anim.fade_out)
    if (duration != null) animation.duration = duration

    this.apply {
        startAnimation(animation)
        isVisible = false
    }
}

fun View.exitEnterView(
    context: Context,
    destination: View,
    forward: Boolean = true
) {
    if (forward) {
        this.viewExitToLeft(context)
        destination.viewEnterFromRight(context)
    } else {
        this.viewExitToRight(context)
        destination.viewEnterFromLeft(context)
    }
}
