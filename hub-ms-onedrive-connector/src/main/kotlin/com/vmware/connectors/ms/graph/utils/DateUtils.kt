package com.vmware.connectors.ms.graph.utils

import com.vmware.connectors.ms.graph.config.CALENDAR
import com.vmware.connectors.ms.graph.config.DATE_FORMAT_PATTERN
import com.vmware.connectors.ms.graph.config.dayLightTimeZones
import com.vmware.connectors.ms.graph.config.windowsTimeZones
import com.vmware.connectors.ms.graph.dto.Hours
import com.vmware.connectors.ms.graph.dto.Minutes
import java.text.SimpleDateFormat
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.*

/**
 * Date Time Formatter object
 */
private val formatter = DateTimeFormatter.ofPattern(DATE_FORMAT_PATTERN)

/**
 * Gives [LocalDateTime] of current date time - [hours](current date time - 24 hours)
 *
 * @param hours
 * @return [String]
 */
fun getDateTimeMinusHours(hours: Long) = LocalDateTime.now().minusHours(hours)

/**
 * Gives [LocalDateTime] of current date time - [hours](current date time - 24 hours)
 *
 * @param minutes
 * @return [String]
 */
fun getDateTimeMinusMinutes(minutes: Long) = LocalDateTime.now().minusMinutes(minutes)

/**
 * Format given date to UTC date time string
 *
 * @param date: Input [LocalDateTime] object
 * @return [String]
 */
fun formatDate(date: LocalDateTime): String = date
        .withNano(0)
        .atZone(ZoneId.systemDefault())
        .withZoneSameInstant(ZoneOffset.UTC)
        .format(formatter)

//fun formatDate(date: Date, userTimeZone: String): String {
//    val formatter = SimpleDateFormat(DATE_FORMAT_PATTERN)
//            .apply { timeZone = TimeZone.getTimeZone(userTimeZone) }
//    return formatter.format(date)
//}


fun parseDate(dateString: String, userTimeZone: String): Date {
    val formatter = SimpleDateFormat(DATE_FORMAT_PATTERN)
            .apply { timeZone = TimeZone.getTimeZone(userTimeZone) }
    return try {
        formatter.parse(dateString)
    } catch (ex: Exception) {
        formatter.parse(formatter.format(Date()))
    }
}

/**
 * this function will return the Date as String with Format [EEE dd-MMM-yy hh:mm a]
 *
 * @param dateString Date Object as String
 * @param timeZone timeZone of the User
 * @returns the Date as String
 */
fun getDateFormatString(dateString: String, timeZone: String): String {
    val formatter = SimpleDateFormat(DATE_FORMAT_PATTERN)
            .apply {
                this.timeZone = TimeZone.getTimeZone("UTC")
            }
    val date = formatter.parse(getDateStringWithRespectToTimeZone(dateString, timeZone))
    val formatter1 = SimpleDateFormat("EEE dd-MMM-yy hh:mm a")
            .apply {
                this.timeZone = TimeZone.getTimeZone("UTC")
            }
    return formatter1.format(date)
}

/**
 * Adds the given number of hours to the present Date
 *
 * @param hours:Input Hours object
 * @return [Date]
 */
operator fun Date.plus(hours: Hours): Date {
    return CALENDAR.also {
        it.timeZone = TimeZone.getTimeZone(hours.zone)
        it.time = this
        it.add(Calendar.HOUR_OF_DAY, hours.hours)
    }.time
}

/**
 * Adds the given number of minutes to the present Date
 *
 * @param minutes:Input Minutes object
 * @return [Date]
 */
operator fun Date.plus(minutes: Minutes): Date {
    return CALENDAR.also {
        it.timeZone = TimeZone.getTimeZone(minutes.zone)
        it.time = this
        it.add(Calendar.MINUTE, minutes.minutes)
    }.time
}

/**
 * This function will return the Date string with respect to the given timezone
 *
 * @param dateString : Date object in UTC as string
 * @param timeZone : represents the timeZone
 * @returns formatted date as string
 */
fun getDateStringWithRespectToTimeZone(dateString: String, timeZone: String): String {
    return if (timeZone in listOf("UTC", "GMT Standard Time", "Greenwich Standard Time"))
        dateString
    else {
        val regex = "\\(UTC(.)(\\d{2}:\\d{2})\\).*".toRegex()
        val matchedList = regex
                .matchEntire(windowsTimeZones.getValue(timeZone))?.groupValues!!
        val (hours, minutes) = matchedList[2].split(":")
        val formatter = SimpleDateFormat(DATE_FORMAT_PATTERN)
                .apply {
                    this.timeZone = TimeZone.getTimeZone("UTC")
                }
        if (matchedList[1] == "+") {
            if (timeZone in dayLightTimeZones) {
                formatter.format(formatter.parse(dateString)
                        + Hours(hours.toInt() + 1, "UTC")
                        + Minutes(minutes.toInt(), "UTC")
                )
            } else {
                formatter.format(formatter.parse(dateString)
                        + Hours(hours.toInt(), "UTC")
                        + Minutes(minutes.toInt(), "UTC")
                )
            }
        } else {
            if (timeZone in dayLightTimeZones) {
                formatter.format(formatter.parse(dateString)
                        + Hours(-hours.toInt() + 1, "UTC")
                        + Minutes(-minutes.toInt(), "UTC")
                )
            } else {
                formatter.format(formatter.parse(dateString)
                        + Hours(-hours.toInt(), "UTC")
                        + Minutes(-minutes.toInt(), "UTC")
                )
            }
        }
    }
}