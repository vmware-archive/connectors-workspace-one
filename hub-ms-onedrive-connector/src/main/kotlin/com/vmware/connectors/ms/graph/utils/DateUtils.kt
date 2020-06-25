/*
* Copyright © 2020 VMware, Inc. All Rights Reserved.
* SPDX-License-Identifier: BSD-2-Clause
*/

package com.vmware.connectors.ms.graph.utils

import com.vmware.connectors.ms.graph.config.DATE_FORMAT_PATTERN
import com.vmware.connectors.ms.graph.config.windowsToIanaMap
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
 * Gives LocalDateTime of current date time - [minutes]
 *
 * @param minutes number of minutes to minus
 * @return [String]
 */
fun getDateTimeMinusMinutes(minutes: Long): LocalDateTime = LocalDateTime.now().minusMinutes(minutes)

/**
 * Format given date to UTC date time string
 *
 * @param date: Input LocalDateTime object
 * @return [String]
 */
fun formatDate(date: LocalDateTime): String = date
        .withNano(0)
        .atZone(ZoneId.systemDefault())
        .withZoneSameInstant(ZoneOffset.UTC)
        .format(formatter)


/**
 * this function will return Date Object with respect to the given dateString
 * and the timeZone
 *
 * @param dateString String Representing Date
 * @param userTimeZone timeZone Of The User
 * @return [Date]
 */
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
 * this function will return the Date as String
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
