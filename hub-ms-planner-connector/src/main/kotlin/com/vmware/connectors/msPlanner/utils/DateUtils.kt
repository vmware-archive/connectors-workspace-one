package com.vmware.connectors.msPlanner.utils

import com.vmware.connectors.msPlanner.config.*
import com.vmware.connectors.msPlanner.dto.DATE_FORMAT_PATTERN
import com.vmware.connectors.msPlanner.dto.Days
import com.vmware.connectors.msPlanner.dto.Hours
import com.vmware.connectors.msPlanner.dto.Minutes
import java.text.SimpleDateFormat
import java.util.*

/**
 * Adds the given number of days to the present Date
 *
 * @param days:Input Days object
 * @return [Date]
 */
operator fun Date.plus(days: Days): Date {
    return CALENDAR.also {
        it.timeZone = TimeZone.getTimeZone(days.zone)
        it.time = this
        it.add(Calendar.DAY_OF_MONTH, days.days)
    }.time
}

/**
 * returns the formatted Date object
 *
 * @param date Date Object
 * @returns the Date Object as String
 */
fun formatDateToString(date: Date): String {
    val formatter = SimpleDateFormat(DATE_FORMAT_PATTERN)
    return formatter.format(date)
}

/**
 * returns the formatted Date object
 *
 * @param date Date Object
 * @returns the Date Object as String
 */
fun formatDateWithOutTime(date: Date): String {
    val formatter = SimpleDateFormat(FORMATTER)
    return formatter.format(date)
}

/**
 * this function will format the string and returns Date Object.
 *
 * @param date Date Object as String
 * @returns the Date Object
 */
fun formatStringToDate(date: String?): Date {
    val formatter = SimpleDateFormat(DATE_FORMAT_PATTERN)
    return formatter.parse(date)
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

/**
 * this function returns the dueDate of the task in user's TimeZone as String.
 *
 * @param dueDate is dueDate of the task.
 * @param timeZone timeZone of the user.
 * @returns the dueDate of the task in user's TimeZone.
 */
fun getUserDueDateInUserTimeZone(
        dueDate: String,
        timeZone: String
): String {
    val dueDateString = getDateStringWithRespectToTimeZone(dueDate, timeZone)
    val dateObject = formatStringToDate(dueDateString)
    val formatter = SimpleDateFormat(RETURN_FORMATTER)
    return formatter.format(dateObject)
}