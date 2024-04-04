package nl.tudelft.trustchain.app

import android.util.Log
import androidx.annotation.ColorRes
import androidx.annotation.DrawableRes
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringSetPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking
import nl.tudelft.trustchain.app.ui.dashboard.DashboardItem
import nl.tudelft.trustchain.common.R
import nl.tudelft.trustchain.common.freedomOfComputing.InstalledApps
import java.util.Collections
import java.util.stream.Collectors

class AppLoader(
    private val dataStore: DataStore<Preferences>,
    private val firstRun: Boolean = false
) {
    val preferredApps: List<DashboardItem>
        get() = apps.filter { it.isPreferred }
    var apps: Set<DashboardItem> = Collections.emptySet()

    @DrawableRes
    val icon = R.drawable.ic_atomic_swap_24dp

    @ColorRes
    val color = R.color.dark_gray

    init {
        runBlocking {
            if (firstRun) {
                // initializes the default installed apps
                setPreferredAppList(DEFAULT_APPS)
            }
            // inject datastore to Installed apps to allow installing foc apps
            InstalledApps.injectDataStore(dataStore, PREFERRED_APPS, INSTALLED_APPS)
            // Update the app list
            update()
        }
    }

    /**
     * Updates the app list, should be called at least whenever a new app is installed
     * or when the preferred apps are changed
     */
    fun update() =
        runBlocking {
            val pApps = getPreferredAppList()
            apps =
                getAllApps().map { app ->
                    DashboardItem(
                        app,
                        isPreferred = pApps.contains(app.appName)
                    )
                }.toSet()
        }

    private suspend fun getAllApps(): Set<AppDefinition> {
        val allApps = AppDefinition.BaseAppDefinitions.entries.map { it.appDefinition }.toMutableSet()
        val installedApps =
            getInstalledAppSet().stream().map {
                FOCAppDefinition(
                    icon,
                    it,
                    color,
                    it
                )
            }.collect(Collectors.toSet())
        allApps.addAll(installedApps)
        return allApps
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
        val preferredApps: Flow<Set<String>> =
            dataStore.data
                .map { preferences ->
                    preferences[PREFERRED_APPS] ?: emptySet()
                }
        preferredApps.first().let {
            return it
        }
    }

    private suspend fun setPreferredAppList(newPreferences: Set<String>) {
        dataStore.edit { settings ->
            settings[PREFERRED_APPS] = newPreferences
        }
        this.apps.forEach {
            it.isPreferred = newPreferences.contains(it.app.appName)
        }
    }

    private suspend fun getInstalledAppSet(): Set<String> {
        val installedApps: Flow<Set<String>> =
            dataStore.data
                .map { preferences ->
                    val res = preferences[INSTALLED_APPS].orEmpty()
                    Log.d("app-installer", "loaded: $res")
                    res
                }
        installedApps.first().let {
            return it
        }
    }

    companion object {
        val PREFERRED_APPS = stringSetPreferencesKey("preferred_apps")
        val INSTALLED_APPS = stringSetPreferencesKey("installed_foc_apps")
        val DEFAULT_APPS =
            setOf(
                AppDefinition.BaseAppDefinitions.CURRENCY_II.appDefinition.appName,
                AppDefinition.BaseAppDefinitions.VALUETRANSFER.appDefinition.appName,
                AppDefinition.BaseAppDefinitions.MUSIC_DAO.appDefinition.appName,
                AppDefinition.BaseAppDefinitions.EUROTOKEN.appDefinition.appName,
                AppDefinition.BaseAppDefinitions.FREEDOM_OF_COMPUTING.appDefinition.appName
            )
    }
}
