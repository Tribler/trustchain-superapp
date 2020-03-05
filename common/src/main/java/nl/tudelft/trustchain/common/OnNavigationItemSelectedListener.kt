package nl.tudelft.trustchain.common

import android.app.Activity
import android.view.MenuItem

interface OnNavigationItemSelectedListener {
    fun onNavigationItemSelected(activity: Activity, item: MenuItem): Boolean
}
