/*
* Copyright © 2020 VMware, Inc. All Rights Reserved.
* SPDX-License-Identifier: BSD-2-Clause
*/

package com.vmware.connectors.sfLeads.config

/**
 * Backend Service API endpoints
 */
object EndpointsAndQueries {
    fun getQueryRecordsUrl(baseUrl: String, query: String) = "$baseUrl/services/data/v47.0/query?q=$query"
    fun getUserDetailsQuery(baseUrl: String) = "$baseUrl/services/oauth2/userinfo"
    fun getRecentLeadsQuery(date: String, ownerId: String) = "select id,name,status,Owner.name,Owner.id,company,email,phone,LastModifiedDate, Description from lead WHERE LastModifiedDate > $date AND Owner.id='$ownerId'"
    fun createTaskForLeadUrl(baseUrl: String) = "$baseUrl/services/data/v47.0/sobjects/Task"
    fun logACallUrl(baseUrl: String) = "$baseUrl/services/data/v47.0/quickActions/LogACall"
}
