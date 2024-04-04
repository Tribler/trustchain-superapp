package nl.tudelft.trustchain.app

import android.content.Intent
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import nl.tudelft.trustchain.app.ui.dashboard.DashboardActivity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AppDefinitionTest {
    @Test
    fun getIntent() {
        val appDefinition = AppDefinition(0, "test_app", 0, DashboardActivity::class.java)
        val context = InstrumentationRegistry.getInstrumentation().targetContext

        val expected = Intent(context, DashboardActivity::class.java)
        val actual = appDefinition.getIntent(context)
        assertEquals(expected.component, actual.component)
        assertNull(actual.extras)
    }

    @Test
    fun getFOCIntent() {
        val appDefinition = FOCAppDefinition(0, "search", 0, "search")
        val context = InstrumentationRegistry.getInstrumentation().targetContext

        val actual = appDefinition.getIntent(context)
        val file = context.cacheDir.resolve("search.apk")
        assertEquals(file.toString(), actual.getStringExtra("fileName"))
    }
}
