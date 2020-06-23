/*
* Copyright © 2020 VMware, Inc. All Rights Reserved.
* SPDX-License-Identifier: BSD-2-Clause
*/

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