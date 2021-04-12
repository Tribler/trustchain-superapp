import androidx.test.ext.junit.runners.AndroidJUnit4
import nl.tudelft.trustchain.gossipML.Essentia
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

@RunWith(AndroidJUnit4::class)
class EssentiaTest {

    @Test
    fun testEssentia() {
        var filepath = File("src/androidTest/assets/GrayMicRecords-LofiDream.mp3").absolutePath
        assertEquals(0, Essentia.extractData(filepath, filepath.replace(".mp3", ".json")))

        filepath = File("src/androidTest/assets/Helen&Shanna-Saudades.mp3").absolutePath
        assertEquals(0, Essentia.extractData(filepath, filepath.replace(".mp3", ".json")))
    }
}
