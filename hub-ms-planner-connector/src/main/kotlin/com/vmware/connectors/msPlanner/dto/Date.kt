package com.vmware.connectors.msPlanner.dto

/**
 * Days object
 *
 * @property days represents the number of days.
 * @property zone represents the timezone.
 */
data class Days(
        val days: Int,
        val zone: String
)

/**
 * Hours object.
 *
 * @property hours represents the number of hours.
 * @property zone represents the zone.
 */
data class Hours(
        val hours: Int,
        val zone: String
)

/**
 * represents the Minutes object.
 *
 * @property minutes represents the minutes.
 * @property zone represents the zone.
 */
data class Minutes(
        val minutes: Int,
        val zone: String
)