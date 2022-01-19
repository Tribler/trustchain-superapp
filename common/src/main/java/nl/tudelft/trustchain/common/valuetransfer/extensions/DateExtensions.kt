package nl.tudelft.trustchain.common.valuetransfer.extensions

import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

fun String.stringToDate(dateFormat: DateFormat): Date? {
    return try {
        dateFormat.parse(this)
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}

fun String.mrzDateToTime(): Long? {
    return this.stringToDate(SimpleDateFormat("yyMMdd", Locale.ENGLISH))?.time
}
