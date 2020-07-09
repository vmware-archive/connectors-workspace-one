/*
* Copyright © 2020 VMware, Inc. All Rights Reserved.
* SPDX-License-Identifier: BSD-2-Clause
*/

package com.vmware.connectors.concur.service

import com.backflipt.commons.*
import com.vmware.connectors.concur.config.Endpoints
import com.vmware.connectors.concur.dto.TravelRequest
import com.vmware.connectors.concur.dto.TravelRequestStatus
import com.vmware.connectors.concur.dto.WorkflowAction
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.core.io.Resource
import org.springframework.http.HttpHeaders.AUTHORIZATION
import org.springframework.http.MediaType.APPLICATION_JSON
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.awaitBody
import java.text.MessageFormat

/**
 * BackendService class contains functions which will fetch the Travel Requests To
 * Be Approved In Concur and the following Actions(Approve ,Send Back to Employee)
 * and every function in the class requires the following parameters
 * baseUrl - tenant url
 * authorization - Sap Concur User Bearer Token
 *
 * @property client: WebClient: library to make async http calls
 */
@Component
class BackendService(
        @Autowired private val client: WebClient,
        @Value("classpath:static/templates/concur-request-template.xml")
        private val concurRequestTemplateResource: Resource
) {

    private val logger = getLogger()
    private val concurRequestTemplate = concurRequestTemplateResource.readAsString()

    /**
     * this Function will return the LoginId of the Concur User
     *
     * @param baseUri is the endPoint to be Called.
     * @param authorization is the token needed for authorizing the call
     * @returns the LoginId of the User
     */
    suspend fun getUserLoginId(
            hubEmailId: String,
            baseUri: String,
            authorization: String
    ): String {
        return client
                .get()
                .uri(Endpoints.getUserLoginIdUrl(baseUri))
                .header(AUTHORIZATION, authorization)
                .accept(APPLICATION_JSON)
                .awaitExchangeAndThrowConcurCustomError {
                    logger.error(it) { "Error in fetching LoginId for hubEmailId -> $hubEmailId" }
                }
                .awaitBody<Map<String, Any>>()
                .getStringOrException("LoginId")
    }

    /**
     * this Function will return the travelRequests that need to be Approved
     *
     * @param userLoginId is loginId of the user
     * @param baseUri is the endPoint to be called.
     * @param authorization is the token needed for authorizing the call
     * @param status is the queryParameter of the endPoint to be called
     * @returns the List of TravelRequest Objects
     */
    private suspend fun getTravelRequests(
            userLoginId: String,
            baseUri: String,
            authorization: String,
            status: String
    ): List<TravelRequest> {
        val (response, time) = measureTimeMillisPair {
            val uri = Endpoints.getTravelRequestsUrl(baseUri, status, userLoginId)
            client
                    .get()
                    .uri(uri)
                    .header(AUTHORIZATION, authorization)
                    .accept(APPLICATION_JSON)
                    .awaitExchangeAndThrowConcurCustomError()
                    .awaitBody<Map<String, Any>>()
                    .getListOrException<Map<String, Any>>("RequestsList")
                    .convertValue<List<TravelRequest>>()
        }
        logger.debug { "Time taken to fetch all travel requests for loginId: $userLoginId: $time ms" }
        return response
    }

    /**
     * this Function will return the details of the TravelRequest
     *
     * @param userLoginId is loginId of the user
     * @param uri is the endPoint to be Called
     * @param authorization is the token needed for authorizing the call
     * @returns the TravelRequestDetails Object
     */
    private suspend fun getTravelRequestDetails(
            userLoginId: String,
            uri: String,
            authorization: String
    ): Map<String, Any> {
        val (response, time) = measureTimeMillisPair {
            client
                    .get()
                    .uri(uri)
                    .header(AUTHORIZATION, authorization)
                    .accept(APPLICATION_JSON)
                    .awaitExchangeAndThrowConcurCustomError()
                    .awaitBody<Map<String, Any>>()
        }
        val requestID = response.getStringOrNull("RequestID")
        val requestName = response.getStringOrNull("RequestName")
        val segmentsList = response.getListOrDefault<Map<String, Any>>("EntriesList").flatMap { entry ->
            entry.getListOrDefault<Map<String, Any>>("SegmentsList")
        }
        logger.debug { "Request travel details for user: $userLoginId, id: ${requestID}, name: ${requestName}, segmentsListCount: ${segmentsList.count()}, time taken: $time ms" }
        return response
    }

    /**
     * this function will perform an Action on TravelRequest
     *
     * @param authorization is the token needed for authorizing the call
     * @param uri is the endPoint to be Called
     * @param action action to be Performed on TravelRequest
     * @param comment comment before Performing the Action
     * @returns the status of the Action
     */
    suspend fun performAction(
            userLoginId: String,
            authorization: String,
            uri: String,
            action: WorkflowAction,
            comment: String?
    ): Boolean {
        val body = MessageFormat.format(
                concurRequestTemplate,
                action.getActionString(),
                comment
        )
        val response = measureTimeMillisPair {
            client
                    .post()
                    .uri(uri)
                    .header(AUTHORIZATION, authorization)
                    .bodyValue(body)
                    .awaitExchangeAndThrowConcurCustomError {
                        logger.error(it) { "Error While Performing ${action.getActionString()} Action With Url ->$uri" }
                    }
                    .statusCode()
                    .is2xxSuccessful
        }
        logger.debug { "Time taken to perform action: $action for loginId -> $userLoginId: ${response.second}" }
        return response.first
    }

    /**
     * Returns all the Travel Requests to be Approved
     *
     * @param userLoginId is loginId of the user
     * @param baseUrl is the endPoint to be called
     * @param authorization is the token needed for authorizing the call
     * @returns the List of TravelRequestDetails Object
     */
    suspend fun getApprovableTravelRequests(
            userLoginId: String,
            baseUrl: String,
            authorization: String
    ): List<Map<String, Any>> {
        val (response, time) = measureTimeMillisPair {
            getTravelRequests(userLoginId, baseUrl, authorization, TravelRequestStatus.TOAPPROVE.name)
                    .mapNotNull {
                        try {
                            getTravelRequestDetails(userLoginId, it.requestDetailsUrl, authorization)
                                    .plus("ApproverLoginID" to it.approverLoginID)
                        } catch (e: Exception) {
                            logger.error(e) { "Error in fetching travel details for user: $userLoginId, id: ${it.requestID}, name: ${it.requestName}, detailsUrl: ${it.requestDetailsUrl} " }
                            null
                        }
                    }
        }
        logger.debug { "Total time taken to fetch all requests information for loginId -> $userLoginId: $time ms" }
        return response
    }
}
