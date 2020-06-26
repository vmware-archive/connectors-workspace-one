/*
* Copyright © 2020 VMware, Inc. All Rights Reserved.
* SPDX-License-Identifier: BSD-2-Clause
*/

package com.vmware.connectors.msTeams.utils

import com.vmware.connectors.msTeams.config.CALENDAR
import com.vmware.connectors.msTeams.config.DATE_FORMAT_PATTERN
import com.vmware.connectors.msTeams.config.windowsToIanaMap
import com.vmware.connectors.msTeams.dto.Minutes
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
 * returns current LocalDateTime of current date time - [hours] hours
 *
 * @param hours represents number of hours that need to be subtracted from given date
 * @return current date time - [hours] hours
 */
fun getDateTimeMinusHours(hours: Long): String = LocalDateTime.now().minusHours(hours)
        .withNano(0)
        .atZone(ZoneId.systemDefault())
        .withZoneSameInstant(ZoneOffset.UTC)
        .format(formatter)

/**
 * returns given Date - [minutes] minutes
 *
 * @receiver date: Date object
 * @param minutes:Input Minutes object
 * @return Gives given Date - [minutes] minutes
 */
operator fun Date.minus(minutes: Minutes): Date {
    return CALENDAR.also {
        it.timeZone = TimeZone.getTimeZone(minutes.zone)
        it.time = this
        it.add(Calendar.MINUTE, (-minutes.minutes))
    }.time
}

/**
 * returns the Date string with respect to the given timezone
 *
 * @param dateString : Date object in UTC as string
 * @param timeZone : represents the timeZone
 * @returns formatted date with respect to the given time zone
 */
fun getDateStringWithRespectToTimeZone(dateString: String, timeZone: String): String {
    val zone = windowsToIanaMap.getOrDefault(timeZone, "UTC")
    val formatter = DateTimeFormatter.ofPattern(DATE_FORMAT_PATTERN)
    return LocalDateTime.parse(dateString, formatter)
            .atZone(ZoneId.of("UTC"))
            .withZoneSameInstant(ZoneId.of(zone))
            .format(formatter)
}