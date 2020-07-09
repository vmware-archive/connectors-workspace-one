/*
* Copyright © 2020 VMware, Inc. All Rights Reserved.
* SPDX-License-Identifier: BSD-2-Clause
*/

package com.vmware.connectors.sfLeads.service

import com.backflipt.commons.*
import com.vmware.connectors.sfLeads.config.EndpointsAndQueries
import com.vmware.connectors.sfLeads.config.LEADS_DATE_FORMATTER
import com.vmware.connectors.sfLeads.config.LEADS_LOOK_SINCE_HOURS
import com.vmware.connectors.sfLeads.dto.Lead
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.awaitBody

/**
 * BackendService class contains functions which will fetch the Modified Leads
 * In the salesForce Account and the following Actions(Logging A Call, Creating Task)
 * and every function in the class requires the following parameters
 * baseUrl - tenant url
 * authorization - SalesForce User Bearer Token
 *
 * @property client: WebClient: library to make async http calls
 */
@Component
class BackendService(
        @Autowired private val client: WebClient
) {
    private val logger = getLogger()

    /**
     * fetches the records for given query
     *
     * @param baseUrl: is the endPoint to be called
     * @param authorization: Connector backend system authorization key
     * @param query : SOQL Query
     * @return list of records
     */
    private suspend fun getQueryRecords(
            baseUrl: String,
            authorization: String,
            query: String
    ): List<Map<String, Any>> {
        val url = EndpointsAndQueries.getQueryRecordsUrl(baseUrl, query)
        return client
                .get()
                .uri(url)
                .header(HttpHeaders.AUTHORIZATION, authorization)
                .awaitExchangeAndThrowSalesForceCustomError {
                    logger.error(it) { "Error While Fetching QueryRecords url:$url" }
                }
                .awaitBody<Map<String, Any>>()
                .getListOrException("records")
    }

    /**
     * fetches the user details of given authorization token
     *
     * @param baseUrl: is the endPoint to be called
     * @param authorization: Connector backend system authorization key
     * @return [Map] object
     */
    private suspend fun getUserDetails(
            baseUrl: String,
            authorization: String
    ): Map<String, Any> {
        val url = EndpointsAndQueries.getUserDetailsQuery(baseUrl)
        return client
                .get()
                .uri(url)
                .header(HttpHeaders.AUTHORIZATION, authorization)
                .awaitExchangeAndThrowSalesForceCustomError {
                    logger.error(it) { "Error while fetching userDetails with url -> $url" }
                }
                .awaitBody()
    }

    /**
     * fetches the leads assigned to the current user.
     *
     * @param baseUrl: is the endPoint to be called
     * @param authorization: Connector backend system authorization key
     * @return list of [Lead] Objects
     */
    suspend fun getRecentLeads(
            baseUrl: String,
            authorization: String
    ): List<Lead> {
        val ownerId = getUserDetails(baseUrl, authorization)
                .getStringOrDefault("user_id")
        val date = getDateTimeMinusHours(LEADS_LOOK_SINCE_HOURS, LEADS_DATE_FORMATTER)
        val query = EndpointsAndQueries.getRecentLeadsQuery(date, ownerId)
        return getQueryRecords(baseUrl, authorization, query)
                .map {
                    val leadId = it.getStringOrDefault("Id")
                    it.plus("link" to "$baseUrl/$leadId")
                }
                .convertValue()
    }

    /**
     * create the task for given lead
     *
     * @param baseUrl: is the endPoint to be called
     * @param authorization: Connector backend system authorization key
     * @param leadId: id of lead
     * @param subject: subject of task
     * @param status: status of task
     * @param priority: priority of task
     * @return status of the creation of task
     */
    suspend fun createTaskForLead(
            baseUrl: String,
            authorization: String,
            leadId: String,
            subject: String,
            status: String? = "Not Started",
            priority: String? = "Normal"
    ): Boolean {
        val url = EndpointsAndQueries.createTaskForLeadUrl(baseUrl)
        val body = mapOf(
                "WhoId" to leadId,
                "Subject" to subject,
                "Status" to status,
                "Priority" to priority
        )
        return client
                .post()
                .uri(url)
                .header("authorization", authorization)
                .header(HttpHeaders.CONTENT_TYPE, "application/json")
                .bodyValue(body)
                .awaitExchangeAndThrowSalesForceCustomError {
                    logger.error(it) { "error while creating Task For Lead: url -> $url" }
                }
                .statusCode()
                .is2xxSuccessful

    }

    /**
     * log A Call for given lead
     *
     * @param authorization: Connector backend system authorization key
     * @param baseUrl: is the endPoint to be called
     * @param leadId: id of owner
     * @param subject: subject of Call
     * @return status of the logging a call
     */
    suspend fun logACall(
            baseUrl: String,
            authorization: String,
            leadId: String,
            subject: String
    ): Boolean {
        val url = EndpointsAndQueries.logACallUrl(baseUrl)
        val callBody = mapOf("record" to mapOf(
                "WhoId" to leadId,
                "Subject" to subject
        )
        )
        return client
                .post()
                .uri(url)
                .bodyValue(callBody)
                .header(HttpHeaders.AUTHORIZATION, authorization)
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .awaitExchangeAndThrowSalesForceCustomError {
                    logger.error(it) { "Error While Logging A Call: url -> $url" }
                }
                .statusCode()
                .is2xxSuccessful
    }
}

