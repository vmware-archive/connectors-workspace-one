/*
* Copyright © 2020 VMware, Inc. All Rights Reserved.
* SPDX-License-Identifier: BSD-2-Clause
*/

package com.vmware.connectors.msPlanner.config

import java.util.*

val CALENDAR: Calendar = Calendar.getInstance()
const val MAX_DUE_DAYS = 14
const val FORMATTER = "yyyy-MM-dd"
const val RETURN_FORMATTER = "EEE dd-MMM-yy"
const val DATE_FORMAT_PATTERN = "yyyy-MM-dd'T'HH:mm:ss'Z'"
const val CONNECTOR_AUTH_MESSAGE_HEADER = "X-Connector-Authorization"
const val MESSAGE_ROUTING_PREFIX = "x-routing-prefix"
const val AUTHORIZATION = "Authorization"
const val CONNECTOR_BASE_URL_PLANNER_HEADER = "X-Connector-Base-Url"

val windowsToIanaMap = mapOf(
        "Afghanistan Standard Time" to "Asia/Kabul",
        "Alaskan Standard Time" to "America/Anchorage",
        "Arab Standard Time" to "Asia/Riyadh",
        "Arabian Standard Time" to "Asia/Dubai",
        "Arabic Standard Time" to "Asia/Baghdad",
        "Argentina Standard Time" to "America/Buenos_Aires",
        "Atlantic Standard Time" to "America/Halifax",
        "AUS Central Standard Time" to "Australia/Darwin",
        "AUS Eastern Standard Time" to "Australia/Sydney",
        "Azerbaijan Standard Time" to "Asia/Baku",
        "Azores Standard Time" to "Atlantic/Azores",
        "Bangladesh Standard Time" to "Asia/Dhaka",
        "Canada Central Standard Time" to "America/Regina",
        "Cape Verde Standard Time" to "Atlantic/Cape_Verde",
        "Caucasus Standard Time" to "Asia/Yerevan",
        "Cen. Australia Standard Time" to "Australia/Adelaide",
        "Central America Standard Time" to "America/Guatemala",
        "Central Asia Standard Time" to "Asia/Almaty",
        "Central Brazilian Standard Time" to "America/Cuiaba",
        "Central Europe Standard Time" to "Europe/Budapest",
        "Central European Standard Time" to "Europe/Warsaw",
        "Central Pacific Standard Time" to "Pacific/Guadalcanal",
        "Central Standard Time Mexico" to "America/Mexico_City",
        "Central Standard Time" to "America/Chicago",
        "China Standard Time" to "Asia/Shanghai",
        "Eastern Standard Time" to "America/New_York",
        "Egypt Standard Time" to "Africa/Cairo",
        "Ekaterinburg Standard Time" to "Asia/Yekaterinburg",
        "Fiji Standard Time" to "Pacific/Fiji",
        "FLE Standard Time" to "Europe/Kiev",
        "Georgian Standard Time" to "Asia/Tbilisi",
        "Greenland Standard Time" to "America/Godthab",
        "Greenwich Standard Time" to "Atlantic/Reykjavik",
        "GTB Standard Time" to "Europe/Istanbul",
        "Hawaiian Standard Time" to "Pacific/Honolulu",
        "India Standard Time" to "Asia/Calcutta",
        "Iran Standard Time" to "Asia/Tehran",
        "Israel Standard Time" to "Asia/Jerusalem",
        "Jordan Standard Time" to "Asia/Amman",
        "Kamchatka Standard Time" to "Asia/Kamchatka",
        "Korea Standard Time" to "Asia/Seoul",
        "Magadan Standard Time" to "Asia/Magadan",
        "Mauritius Standard Time" to "Indian/Mauritius",
        "Middle East Standard Time" to "Asia/Beirut",
        "Montevideo Standard Time" to "America/Montevideo",
        "Morocco Standard Time" to "Africa/Casablanca",
        "Mountain Standard Time Mexico" to "America/Chihuahua",
        "Mountain Standard Time" to "America/Denver",
        "Myanmar Standard Time" to "Asia/Rangoon",
        "N. Central Asia Standard Time" to "Asia/Novosibirsk",
        "Namibia Standard Time" to "Africa/Windhoek",
        "Nepal Standard Time" to "Asia/Katmandu",
        "New Zealand Standard Time" to "Pacific/Auckland",
        "Newfoundland Standard Time" to "America/St_Johns",
        "North Asia East Standard Time" to "Asia/Irkutsk",
        "North Asia Standard Time" to "Asia/Krasnoyarsk",
        "Pacific SA Standard Time" to "America/Santiago",
        "Pacific Standard Time Mexico" to "America/Tijuana",
        "Pacific Standard Time" to "America/Los_Angeles",
        "Pakistan Standard Time" to "Asia/Karachi",
        "Paraguay Standard Time" to "America/Asuncion",
        "Romance Standard Time" to "Europe/Paris",
        "Russian Standard Time" to "Europe/Moscow",
        "SA Eastern Standard Time" to "America/Cayenne",
        "SA Pacific Standard Time" to "America/Bogota",
        "SA Western Standard Time" to "America/La_Paz",
        "Samoa Standard Time" to "Pacific/Samoa",
        "SE Asia Standard Time" to "Asia/Bangkok",
        "Singapore Standard Time" to "Asia/Singapore",
        "South Africa Standard Time" to "Africa/Johannesburg",
        "Sri Lanka Standard Time" to "Asia/Colombo",
        "Syria Standard Time" to "Asia/Damascus",
        "Taipei Standard Time" to "Asia/Taipei",
        "Tasmania Standard Time" to "Australia/Hobart",
        "Tokyo Standard Time" to "Asia/Tokyo",
        "Tonga Standard Time" to "Pacific/Tongatapu",
        "Ulaanbaatar Standard Time" to "Asia/Ulaanbaatar",
        "US Eastern Standard Time" to "America/Indianapolis",
        "US Mountain Standard Time" to "America/Phoenix",
        "Venezuela Standard Time" to "America/Caracas",
        "Vladivostok Standard Time" to "Asia/Vladivostok",
        "W. Australia Standard Time" to "Australia/Perth",
        "W. Central Africa Standard Time" to "Africa/Lagos",
        "W. Europe Standard Time" to "Europe/Berlin",
        "West Asia Standard Time" to "Asia/Tashkent",
        "West Pacific Standard Time" to "Pacific/Port_Moresby",
        "Yakutsk Standard Time" to "Asia/Yakutsk"
)
