package nl.tudelft.trustchain.app

import android.content.Context
import android.content.Intent
import nl.tudelft.trustchain.foc.ExecutionActivity

/**
 * A definition of a sub-app, which can be shown in the [nl.tudelft.trustchain.app.ui.dashboard.DashboardActivity]
 *
 * This application is loaded from an apk file using the [nl.tudelft.trustchain.foc] sub-module.
 */
class FOCAppDefinition(
    icon: Int,
    appName: String,
    color: Int,
    val fileName: String,
    disableImageTint: Boolean = false
) : AppDefinition(icon, appName, color, ExecutionActivity::class.java, disableImageTint) {
    override fun getIntent(context: Context): Intent {
        val intent = super.getIntent(context)
        intent.putExtra("fileName", "${context.cacheDir}/${fileName.split("/").last()}.apk")
        return intent
    }
}
