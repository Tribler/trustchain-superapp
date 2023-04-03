package nl.tudelft.trustchain.detoks.helpers

import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.*

class DateFormatter {

    companion object {
        private val formatter = SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH)

        init {
            formatter.timeZone = TimeZone.getTimeZone("GMT")
        }

        fun stringToDate(date: String): Date {

            val parsedDate: Date = try {
                formatter.parse(date) as Date
            } catch (e: ParseException) {
                // TODO Error handling
                Date()
            }

            return parsedDate
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
                //            System.out.println("Local Millis * " + date.getTime() + "  ---UTC time  " + strDate);//correct
                val utcDate = formatter.parse(strDate)
                //            System.out.println("UTC Millis * " + utcDate.getTime() + " ------  " + dateFormatLocal.format(utcDate));
                return utcDate!!
            } catch (e: Exception) {
                e.printStackTrace()
            }
            return Date(time)
        }


    }
}
