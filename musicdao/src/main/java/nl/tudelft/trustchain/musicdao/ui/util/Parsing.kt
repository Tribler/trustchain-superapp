package nl.tudelft.trustchain.musicdao.ui.util

import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.*

fun dateToShortString(instant: String): String {
    return try {
        val time = Instant.parse(instant)
        val result =
            DateTimeFormatter.ofPattern("MMM uuuu")
                .withLocale(Locale.UK)
                .withZone(ZoneId.systemDefault())
                .format(time)
        result
    } catch (e: Exception) {
        ""
    }
}

fun dateToLongString(instant: String): String {
    return try {
        val time = Instant.parse(instant)
        val result =
            DateTimeFormatter.ofLocalizedDateTime(FormatStyle.SHORT)
                .withLocale(Locale.UK)
                .withZone(ZoneId.systemDefault())
                .format(time)
        result
    } catch (e: Exception) {
        ""
    }
}
