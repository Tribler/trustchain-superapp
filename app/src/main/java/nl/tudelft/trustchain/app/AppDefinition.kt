package nl.tudelft.trustchain.app

import android.app.Activity
import androidx.annotation.ColorRes
import androidx.annotation.DrawableRes
import nl.tudelft.trustchain.common.R
import nl.tudelft.trustchain.debug.DebugActivity
import nl.tudelft.trustchain.explorer.ui.TrustChainExplorerActivity

enum class AppDefinition(
    @DrawableRes val icon: Int,
    val appName: String,
    @ColorRes val color: Int,
    val activity: Class<out Activity>
) {
    TRUSTCHAIN_EXPLORER(
        R.drawable.ic_device_hub_black_24dp,
        "TrustChain Explorer",
        R.color.red,
        TrustChainExplorerActivity::class.java
    ),
    DEBUG(
        R.drawable.ic_bug_report_black_24dp,
        "Debug",
        R.color.dark_gray,
        DebugActivity::class.java
    )
}
