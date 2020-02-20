package com.vmware.connectors.msTeams.dto

/**
 * Hours object
 */
data class Hours(
        /**
         * represents the number of hours
         */
        val hours: Int,
        /**
         * represents the zone
         */
        val zone: String
)

/**
 * represents the Minutes object
 */
data class Minutes(
        /**
         * represents the minutes
         */
        val minutes: Int,
        /**
         * represents the zone
         */
        val zone: String
)