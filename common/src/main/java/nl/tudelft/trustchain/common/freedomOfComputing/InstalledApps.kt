package nl.tudelft.trustchain.common.freedomOfComputing

import android.util.Log
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import kotlinx.coroutines.runBlocking
import java.util.Collections

/**
 * Singleton object for installing FOC apps on the home screen
 */
object InstalledApps {
    // A temporary storage to use for to install app names while the datastore has not been injected yet.
    private val appNames: MutableSet<String> = HashSet()

    // A temporary storage to use for preferred app names while the datastore has not been injected yet.
    private val preferredApps: MutableSet<String> = HashSet()

    // The datastore that should be injected from the :app submodule.
    private var dataStore: DataStore<Preferences>? = null

    // The key to use for storing preferred apps.
    private var preferredAppsKey: Preferences.Key<Set<String>>? = null

    // The key to use for storing foc installed apps.
    private var installedappskey: Preferences.Key<Set<String>>? = null

    /**
     * Should be called with the datastore from the home screen, to install apps.
     *
     * @param dataStore The datastore in which to store the app preferences
     * @param preferredAppsKey The key to use for assigning apps preference
     * @param installedAppsKey The key to use for denoting that the app is installed
     */
    fun injectDataStore(
        dataStore: DataStore<Preferences>,
        preferredAppsKey: Preferences.Key<Set<String>>,
        installedAppsKey: Preferences.Key<Set<String>>
    ) {
        this.preferredAppsKey = preferredAppsKey
        this.installedappskey = installedAppsKey
        this.dataStore = dataStore
        if (appNames.size > 0) {
            install(HashSet(appNames), HashSet(preferredApps))
            appNames.clear()
            preferredApps.clear()
        }
    }

    /**
     * Adds app to the home screen.
     * Or stores it for later installation, if [injectDataStore] has not yet been called.
     *
     * @param appName The name of the app (should be the same as their name on disk - '.apk')
     * @param preferred Whether to set the app to be preferred
     */
    fun addApp(
        appName: String,
        preferred: Boolean = true
    ) {
        if (dataStore == null) {
            appNames.add(appName)
            if (preferred) {
                preferredApps.add(appName)
            }
        } else {
            install(appName, preferred)
        }
    }

    /**
     * Installs all apps in one go into the [dataStore].
     *
     * @param appNames The name of the apps to install (should be the same as their name on disk - '.apk')
     * @param preferredApps The names of all newly preferred apps
     */
    private fun install(
        appNames: Collection<String>,
        preferredApps: Collection<String>
    ) {
        Log.i("app-installer", "Installing apps: $appNames, $preferredApps")
        runBlocking {
            dataStore!!.edit { settings ->
                val set = settings[installedappskey!!].orEmpty().toMutableSet()
                set.addAll(appNames)
                settings[installedappskey!!] = set

                if (!preferredApps.isEmpty()) {
                    val oldPreferred = settings[preferredAppsKey!!].orEmpty().toMutableSet()
                    oldPreferred.addAll(preferredApps)
                    settings[preferredAppsKey!!] = oldPreferred
                }
            }
        }
    }

    /**
     * Installs a single apps into the [dataStore].
     *
     * @param appName The name of the apps to install (should be the same as their name on disk - '.apk')
     * @param preferred A boolean indicating the app should be set as prefered
     */
    private fun install(
        appName: String,
        preferred: Boolean
    ) {
        val collection = Collections.singleton(appName)
        if (preferred) {
            install(collection, collection)
        } else {
            install(collection, Collections.emptySet())
        }
    }

    /**
     * Function used for testing for resetting this object to its initial state.
     */
    fun reset() {
        this.appNames.clear()
        this.preferredApps.clear()

        this.dataStore = null
        this.preferredAppsKey = null
        this.installedappskey = null
    }
}
