/*
* Copyright © 2020 VMware, Inc. All Rights Reserved.
* SPDX-License-Identifier: BSD-2-Clause
*/

package com.vmware.connectors.concur.config

/**
 * Backend Service API endpoints
 */
object Endpoints {
    fun getTravelRequestsUrl(baseUrl: String, status: String, loginId: String) = "$baseUrl/api/travelrequest/v1.0/requestslist/?status=$status&loginid=$loginId"
    fun getUserLoginIdUrl(baseUrl: String) = "$baseUrl/api/user/v1.0/user"
}
