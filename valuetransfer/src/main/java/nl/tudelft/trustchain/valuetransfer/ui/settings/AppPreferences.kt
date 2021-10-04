package nl.tudelft.trustchain.valuetransfer.ui.settings

import android.content.Context
import android.content.SharedPreferences
import androidx.appcompat.app.AppCompatDelegate
import nl.tudelft.trustchain.valuetransfer.R
import nl.tudelft.trustchain.valuetransfer.ValueTransferMainActivity

class AppPreferences(
    private val parentActivity: ValueTransferMainActivity
) {

    private var sharedPreferences: SharedPreferences = parentActivity.getSharedPreferences(PREFS_FILE_NAME, Context.MODE_PRIVATE)

    fun getCurrentTheme(): String? {
        return sharedPreferences.getString(PREFS_THEME_NAME, APP_THEME_DAY)
    }

    fun setTheme(theme: String) {
        sharedPreferences.edit().putString(
            PREFS_THEME_NAME,
            theme
        ).apply()
    }

    fun switchTheme(theme: String?) {
        when (theme) {
            APP_THEME_DAY -> setThemeDay()
            APP_THEME_NIGHT -> setThemeNight()
            APP_THEME_SYSTEM -> setThemeSystem()
            else -> setThemeDay()
        }
    }

    companion object {
        const val PREFS_FILE_NAME = "prefs_vt"
        const val PREFS_THEME_NAME = "theme"

        val APP_THEME = R.style.Theme_ValueTransfer
        const val APP_THEME_DAY = "day"
        const val APP_THEME_NIGHT = "night"
        const val APP_THEME_SYSTEM = "system"

        private lateinit var instance: AppPreferences
        fun getInstance(parentActivity: ValueTransferMainActivity): AppPreferences {
            if (!::instance.isInitialized) {
                instance = AppPreferences(parentActivity)
            }
            return instance
        }

        private fun setThemeDay() {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
        }

        private fun setThemeNight() {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
        }

        private fun setThemeSystem() {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
        }

    }
}
