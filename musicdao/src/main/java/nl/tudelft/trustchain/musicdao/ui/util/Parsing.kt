package nl.tudelft.trustchain.musicdao.ui.util

import android.os.Build
import androidx.annotation.RequiresApi
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.*

@RequiresApi(Build.VERSION_CODES.O)
fun dateToShortString(instant: String): String {
    return try {
        val time = Instant.parse(instant)
        val result = DateTimeFormatter.ofPattern("MMM uuuu")
            .withLocale(Locale.UK)
            .withZone(ZoneId.systemDefault())
            .format(time)
        result
    } catch (e: Exception) {
        ""
    }
}

@RequiresApi(Build.VERSION_CODES.O)
fun dateToLongString(instant: String): String {
    return try {
        val time = Instant.parse(instant)
        val result = DateTimeFormatter.ofLocalizedDateTime(FormatStyle.SHORT)
            .withLocale(Locale.UK)
            .withZone(ZoneId.systemDefault())
            .format(time)
        result
    } catch (e: Exception) {
        ""
    }
}
