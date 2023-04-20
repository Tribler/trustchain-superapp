package nl.tudelft.trustchain.debug.helpers

import nl.tudelft.trustchain.detoks.helpers.DateFormatter
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.lang.NumberFormatException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

class DateFormatterTest {
    private lateinit var formatter: SimpleDateFormat

    @Before
    fun setUp() {
        formatter = SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH)
        formatter.timeZone = TimeZone.getTimeZone("GMT")
    }

    @Test
    fun testStringToDate() {
        val dateString = "2023-04-18"

        val result = DateFormatter.stringToDate(dateString)

        assertNotNull(result)
        assertEquals(dateString, formatter.format(result))
    }

    @Test
    fun testStringToDateWithInvalidFormat() {
        val dateString = "18-04-2023"

        val result = DateFormatter.stringToDate(dateString)

        assertNotNull(result)
        assertNotEquals(dateString, formatter.format(result))
    }

    @Test
    fun testDateToString() {
        val date = Date()

        val result = DateFormatter.dateToString(date)

        assertNotNull(result)
    }

    @Test
    fun testTodayAsString() {
        val currentDate = Date()
        val expectedFormattedDate = formatter.format(currentDate)

        val result = DateFormatter.todayAsString()

        assertNotNull(result)
        assertEquals(expectedFormattedDate, result)
    }

    @Test
    fun testStartOfToday() {
        val currentDate = Date()
        val expectedFormattedDate = formatter.format(currentDate)
        val expectedDate = formatter.parse(expectedFormattedDate) as Date

        val result = DateFormatter.startOfToday()

        assertNotNull(result)
        assertEquals(expectedDate, result)
    }

    @Test
    fun testLocalTimeToGMTDate() {
        val currentTimeMillis = System.currentTimeMillis()
        val expectedFormattedDate = formatter.format(Date(currentTimeMillis))
        val expectedDate = formatter.parse(expectedFormattedDate) as Date

        val result = DateFormatter.localTimeToGMTDate(currentTimeMillis)

        assertNotNull(result)
        assertEquals(expectedDate, result)
    }

    @Test(expected = NumberFormatException::class)
    fun testLocalTimeToGMTDateWithNumberFormatException() {
        val invalidTimeMillis = "invalid_date_string"

        DateFormatter.localTimeToGMTDate(invalidTimeMillis.toLong())
    }
}
