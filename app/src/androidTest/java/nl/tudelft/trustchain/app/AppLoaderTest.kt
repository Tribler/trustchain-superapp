package nl.tudelft.trustchain.app

import android.content.Context
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStoreFile
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.runBlocking
import nl.tudelft.trustchain.common.freedomOfComputing.InstalledApps
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.util.UUID

@RunWith(AndroidJUnit4::class)
class AppLoaderTest {
    private val testDataStoreName: String = UUID.randomUUID().toString()
    private val context: Context = InstrumentationRegistry.getInstrumentation().targetContext
    private val dataStore =
        PreferenceDataStoreFactory.create(
            produceFile =
                { context.preferencesDataStoreFile(testDataStoreName) }
        )

    @After
    fun cleanUp() {
        runBlocking {
            dataStore.edit { it.clear() }
        }
    }

    @Test
    fun noneExtraInstalled() {
        val appLoader = AppLoader(dataStore, true)
        val apps = appLoader.apps
        val appDefinitions = apps.filter { it.isPreferred }.map { it.app.appName }.toSet()

        val expected = AppLoader.DEFAULT_APPS
        assertEquals(expected, appDefinitions)
    }

    @Test
    fun oneInstalled() {
        val appLoader = AppLoader(dataStore, true)
        val appName = "search"
        var appDefinitions = appLoader.apps.map { it.app }.toSet()

        assertFalse("$appName should not be in default installed apps", appDefinitions.any { app -> app.appName == appName })

        InstalledApps.addApp(appName)
        appLoader.update()

        appDefinitions = appLoader.apps.map { it.app }.toSet()

        assertTrue("$appName should be in apps after installing", appDefinitions.any { app -> app.appName == appName })
    }
}
