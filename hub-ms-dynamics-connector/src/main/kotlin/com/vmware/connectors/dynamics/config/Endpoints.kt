/*
* Copyright © 2020 VMware, Inc. All Rights Reserved.
* SPDX-License-Identifier: BSD-2-Clause
*/

package com.vmware.connectors.dynamics.config

/**
 * Backend Service API endpoints
 */

object Endpoints {
    fun getMyProfileUrl(baseURI: String) = "$baseURI/$ODATA_API_PREFIX/WhoAmI"
    fun getMyAccountsUrl(baseURI: String) = "$baseURI/$ODATA_API_PREFIX/accounts"
    fun getPrimaryContactsForAccounts(baseURI: String) = "$baseURI/$ODATA_API_PREFIX/contacts"
    fun getTasksUrl(baseURI: String) = "$baseURI/$ODATA_API_PREFIX/tasks"
    fun getAppointmentsUrl(baseURI: String) = "$baseURI/$ODATA_API_PREFIX/appointments"
    fun getPhoneCallsUrl(baseURI: String) = "$baseURI/$ODATA_API_PREFIX/phonecalls"
}