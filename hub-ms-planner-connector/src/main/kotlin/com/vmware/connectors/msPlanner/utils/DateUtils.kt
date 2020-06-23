/*
* Copyright © 2020 VMware, Inc. All Rights Reserved.
* SPDX-License-Identifier: BSD-2-Clause
*/

package com.vmware.connectors.msPlanner.utils

import com.vmware.connectors.msPlanner.config.CALENDAR
import com.vmware.connectors.msPlanner.config.DATE_FORMAT_PATTERN
import com.vmware.connectors.msPlanner.config.RETURN_FORMATTER
import com.vmware.connectors.msPlanner.config.windowsToIanaMap
import com.vmware.connectors.msPlanner.dto.Days
import java.text.SimpleDateFormat
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.*


/**
 * Adds the given number of days to the Date
 *
 * @param days:Input Days object
 * @return [Date] given date + [days]
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
 * this function will format the string and returns Date Object.
 *
 * @param date Date Object as String
 * @returns [Date]
 */
fun formatStringToDate(date: String?): Date {
    val formatter = SimpleDateFormat(DATE_FORMAT_PATTERN)
    return formatter.parse(date)
}

/**
 * This function will return the Date string with respect to the given timezone
 *
 * @param dateString : Date object in UTC as string
 * @param timeZone : represents the timeZone
 * @returns formatted date as string
 */
fun getDateStringWithRespectToTimeZone(dateString: String, timeZone: String): String {
    val zone = windowsToIanaMap.getOrDefault(timeZone, "UTC")
    val formatter = DateTimeFormatter.ofPattern(DATE_FORMAT_PATTERN)
    return LocalDateTime.parse(dateString, formatter)
            .atZone(ZoneId.of("UTC"))
            .withZoneSameInstant(ZoneId.of(zone))
            .format(formatter)
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
        timeZone: String,
        formatter: String = RETURN_FORMATTER
): String {
    val dueDateString = getDateStringWithRespectToTimeZone(dueDate, timeZone)
    val dateObject = formatStringToDate(dueDateString)
    return SimpleDateFormat(formatter)
            .format(dateObject)
}

/**
 * This Function will return the current Time in UTC TimeZone
 */
fun getCurrentUtcTime(): String {
    val simpleDateFormat = SimpleDateFormat(DATE_FORMAT_PATTERN)
    simpleDateFormat.timeZone = TimeZone.getTimeZone("UTC")
    return simpleDateFormat.format(Date())
}
