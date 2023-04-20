package nl.tudelft.trustchain.detoks.helpers

import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.*

object DateFormatter {

        private val formatter = SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH)

        init {
            formatter.timeZone = TimeZone.getTimeZone("GMT")
        }

        fun stringToDate(date: String): Date {

            val parsedDate: Date = try {
                formatter.parse(date) as Date
            } catch (e: ParseException) {
                Date()
            }

            return parsedDate
        }

        fun dateToString(date: Date): String {
            return formatter.format(date)
        }

        fun todayAsString() : String {
            val today = Date()
            return formatter.format(today)
        }

        fun startOfToday() : Date {
            val today = Date()
            val todayString = formatter.format(today)
            return formatter.parse(todayString) as Date
        }

        fun localTimeToGMTDate(time: Long): Date {
            try {
                val date = Date(time)
                val strDate = formatter.format(date)
                val gmtDate = formatter.parse(strDate)
                return gmtDate!!
            } catch (e: Exception) {
                e.printStackTrace()
            }
            return Date(time)
        }

}
