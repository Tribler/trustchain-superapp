package nl.tudelft.trustchain.app

import android.annotation.SuppressLint
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringSetPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking
import nl.tudelft.trustchain.app.ui.dashboard.DashboardItem

class AppLoader(
    private val dataStore: DataStore<Preferences>,
    private val firstRun: Boolean = false
) {

    val preferredApps: List<DashboardItem>
        get() = apps.filter { it.isPreferred }
    var apps: Set<DashboardItem>

    init {
        runBlocking {
            if (firstRun) {
                apps = AppDefinition.values().map { DashboardItem(it) }.toSet()
                setPreferredAppList(DEFAULT_APPS)
            } else {
                val pApps = getPreferredAppList()
                apps = AppDefinition.values().map { app ->
                    DashboardItem(
                        app,
                        isPreferred = pApps.contains(app.appName)
                    )
                }.toSet()
            }
        }
    }

    suspend fun setPreferredApp(app: String) {
        val newApps = preferredApps.map { it.app.appName }.toMutableSet()
        newApps.add(app)
        return setPreferredAppList(newApps)
    }

    suspend fun removePreferredApp(app: String) {
        val newApps = preferredApps.map { it.app.appName }.toMutableSet()
        newApps.remove(app)
        return setPreferredAppList(newApps)
    }

    private suspend fun getPreferredAppList(): Set<String> {
        val preferredApps: Flow<Set<String>> = dataStore.data
            .map { preferences ->
                preferences[PREFERRED_APPS] ?: emptySet()
            }
        preferredApps.first().let {
            return it
        }
    }

    @SuppressLint("NewApi")
    private suspend fun setPreferredAppList(newPreferences: Set<String>) {
        dataStore.edit { settings ->
            settings[PREFERRED_APPS] = newPreferences
        }
        this.apps.forEach {
            it.isPreferred = newPreferences.contains(it.app.appName)
        }
    }

    companion object {
        val PREFERRED_APPS = stringSetPreferencesKey("preferred_apps")
        val DEFAULT_APPS = setOf(
            AppDefinition.DETOKS.appName,
//            AppDefinition.VALUETRANSFER.appName,
//            AppDefinition.MUSIC_DAO.appName,
//            AppDefinition.EIGHTEEN_PLUS.appName
        )
    }
}
