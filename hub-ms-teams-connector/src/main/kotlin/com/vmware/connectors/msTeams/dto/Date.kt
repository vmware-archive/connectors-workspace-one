/*
* Copyright © 2020 VMware, Inc. All Rights Reserved.
* SPDX-License-Identifier: BSD-2-Clause
*/

package com.vmware.connectors.msTeams.dto

/**
 * represents the Minutes object
 *
 * @property minutes represents the minutes
 * @property zone represents the time zone
 */
data class Minutes(
        val minutes: Int,
        val zone: String
)