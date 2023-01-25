package nl.tudelft.trustchain.app.ui.dashboard

import com.mattskala.itemadapter.Item
import nl.tudelft.trustchain.app.AppDefinition

class DashboardItem(
    val app: AppDefinition,
    var isPreferred: Boolean = false
) : Item()
