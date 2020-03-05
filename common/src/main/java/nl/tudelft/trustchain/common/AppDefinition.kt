package nl.tudelft.trustchain.common

import android.app.Activity
import androidx.annotation.ColorRes
import androidx.annotation.DrawableRes

enum class AppDefinition(
    @DrawableRes val icon: Int,
    val appName: String,
    @ColorRes val color: Int
) {
    TRUSTCHAIN_EXPLORER(R.drawable.ic_device_hub_black_24dp, "TrustChain Explorer", R.color.red),
    DEBUG(R.drawable.ic_bug_report_black_24dp, "Debug", R.color.dark_gray)
}
