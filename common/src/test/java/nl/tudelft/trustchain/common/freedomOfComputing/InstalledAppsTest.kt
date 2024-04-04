package nl.tudelft.trustchain.common.freedomOfComputing

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import io.mockk.every
import io.mockk.mockk
import io.mockk.spyk
import io.mockk.verify
import org.junit.Before
import org.junit.Test
import java.util.Collections

const val INSTALL_FUNC = "install"

class InstalledAppsTest {
    private var installedApps = InstalledApps
    private lateinit var dataStoreMock: DataStore<Preferences>
    private var preferredAppsKey: Preferences.Key<Set<String>> = mockk()
    private var installedappskey: Preferences.Key<Set<String>> = mockk()

    @Before
    fun setup() {
        installedApps.reset()
        dataStoreMock = mockk<DataStore<Preferences>>()
        installedApps = spyk(InstalledApps, recordPrivateCalls = true)
        every { installedApps[INSTALL_FUNC](any<Collection<String>>(), any<Collection<String>>()) } returns 0
    }

    @Test
    fun addBeforeInjectWithNoPreference() {
        installedApps.addApp("search", preferred = false)

        verify(exactly = 0) {
            installedApps[INSTALL_FUNC](any<Collection<String>>(), any<Collection<String>>())
        }

        installedApps.injectDataStore(dataStoreMock, preferredAppsKey, installedappskey)

        val expectedNames = Collections.singleton("search")
        val expectedPref = Collections.emptySet<String>()

        verify {
            installedApps[INSTALL_FUNC](expectedNames, expectedPref)
        }
    }

    @Test
    fun addAfterInjectWithNoPreference() {
        installedApps.injectDataStore(dataStoreMock, preferredAppsKey, installedappskey)

        verify(exactly = 0) {
            installedApps[INSTALL_FUNC](any<Collection<String>>(), any<Collection<String>>())
        }

        installedApps.addApp("search", preferred = false)

        val expectedNames = Collections.singleton("search")
        val expectedPref = Collections.emptySet<String>()
        verify {
            installedApps[INSTALL_FUNC](expectedNames, expectedPref)
        }
    }

    @Test
    fun addBeforeInjectWithPreference() {
        installedApps.addApp("search")

        verify(exactly = 0) {
            installedApps[INSTALL_FUNC](any<Collection<String>>(), any<Collection<String>>())
        }

        installedApps.injectDataStore(dataStoreMock, preferredAppsKey, installedappskey)

        val expectedNames = Collections.singleton("search")

        verify {
            installedApps[INSTALL_FUNC](expectedNames, expectedNames)
        }
    }

    @Test
    fun addAfterInjectWithPreference() {
        installedApps.injectDataStore(dataStoreMock, preferredAppsKey, installedappskey)

        verify(exactly = 0) {
            installedApps[INSTALL_FUNC](any<Collection<String>>(), any<Collection<String>>())
        }

        installedApps.addApp("search")

        val expectedNames = Collections.singleton("search")
        verify {
            installedApps[INSTALL_FUNC](expectedNames, expectedNames)
        }
    }
}
