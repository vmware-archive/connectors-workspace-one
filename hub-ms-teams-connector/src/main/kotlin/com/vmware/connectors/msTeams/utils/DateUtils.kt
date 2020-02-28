package com.vmware.connectors.msTeams.utils

import com.vmware.connectors.msTeams.config.dayLightTimeZones
import com.vmware.connectors.msTeams.config.windowsTimeZones
import com.vmware.connectors.msTeams.config.CALENDAR
import com.vmware.connectors.msTeams.config.DATE_FORMAT_PATTERN
import com.vmware.connectors.msTeams.dto.Hours
import com.vmware.connectors.msTeams.dto.Minutes
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
fun getDateTimeMinusHours(hours: Long): String = LocalDateTime.now().minusHours(hours)
        .withNano(0)
        .atZone(ZoneId.systemDefault())
        .withZoneSameInstant(ZoneOffset.UTC)
        .format(formatter)

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
 * Subtracts the given number of hours in the present Date
 *
 * @param hours:Input Hours object
 * @return [Date]
 */
operator fun Date.minus(hours: Hours): Date {
    return CALENDAR.also {
        it.timeZone = TimeZone.getTimeZone(hours.zone)
        it.time = this
        it.add(Calendar.HOUR_OF_DAY, (-hours.hours))
    }.time
}

/**
 * Subtracts the given number of minutes in the present Date
 *
 * @param minutes:Input Minutes object
 * @return [Date]
 */
operator fun Date.minus(minutes: Minutes): Date {
    return CALENDAR.also {
        it.timeZone = TimeZone.getTimeZone(minutes.zone)
        it.time = this
        it.add(Calendar.MINUTE, (-minutes.minutes))
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