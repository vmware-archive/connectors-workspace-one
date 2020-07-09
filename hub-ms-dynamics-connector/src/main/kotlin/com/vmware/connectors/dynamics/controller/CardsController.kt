/*
* Copyright © 2020 VMware, Inc. All Rights Reserved.
* SPDX-License-Identifier: BSD-2-Clause
*/

package com.vmware.connectors.dynamics.controller

import com.backflipt.commons.getLogger
import com.backflipt.commons.serialize
import com.vmware.connectors.common.utils.AuthUtil
import com.vmware.connectors.common.utils.CommonUtils
import com.vmware.connectors.dynamics.config.AUTHORIZATION
import com.vmware.connectors.dynamics.config.CONNECTOR_AUTH_HEADER
import com.vmware.connectors.dynamics.config.CONNECTOR_BASE_URL_HEADER
import com.vmware.connectors.dynamics.config.ROUTING_PREFIX
import com.vmware.connectors.dynamics.dto.CreateAppointmentRequest
import com.vmware.connectors.dynamics.dto.CreatePhoneCallRequest
import com.vmware.connectors.dynamics.dto.CreateTaskRequest
import com.vmware.connectors.dynamics.service.BackendService
import com.xenovus.explang.generateCards
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.MediaType.APPLICATION_JSON_VALUE
import org.springframework.http.ResponseEntity
import org.springframework.http.server.reactive.ServerHttpRequest
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RestController
import javax.validation.Valid

/**
 * Cards Request Rest API Controller
 *
 * This Controller has 4 REST endpoints
 * 1. /cards/requests
 * 2. /accounts/{accountId}/create_task
 * 3. /accounts/{accountId}/schedule_appointment
 * 4. /accounts/{accountId}/log_phonecall
 *
 * @property service MSDynamicsService: service to communicate with Microsoft Dynamics backend API
 * @constructor creates Rest Controller
 */
@RestController
class CardsController(
        @Autowired private val service: BackendService
) {
    private val logger = getLogger()

    /**
     * Generates Card Objects for Accounts that are assigned to a user in MS Dynamics
     *
     * @param token: Connector user token
     * @param authorization: Connector user token that is passed as a header(X-Connector-Authorization) in the request
     * @param baseURL: Connector base url that is passed as a header(X-Connector-Base-Url) in the request
     * @param routingPrefix: Connector routing prefix, this is used if the request is proxied from a reverse-proxy/load-balancer to make the card action urls
     * @param request: ServerHttpRequest
     * @return ResponseEntity<Any>: list of newly created accounts as cards for the user
     */
    @PostMapping(path = ["/cards/requests"], produces = [APPLICATION_JSON_VALUE], consumes = [APPLICATION_JSON_VALUE])
    suspend fun getCards(
            @RequestHeader(name = AUTHORIZATION) token: String,
            @RequestHeader(name = CONNECTOR_AUTH_HEADER) authorization: String,
            @RequestHeader(name = CONNECTOR_BASE_URL_HEADER) baseURL: String,
            @RequestHeader(name = ROUTING_PREFIX) routingPrefix: String,
            request: ServerHttpRequest
    ): ResponseEntity<Any> {
        val hubUserMail = AuthUtil.extractUserEmail(token)
        logger.debug { "received cards request for $hubUserMail with baseUrl-> $baseURL" }
        val accounts = service.fetchNewAccountsCreatedSince24Hours(baseURL, authorization)
                .serialize()
        val cards = generateCards(accounts, mapOf(
                "routing_prefix" to routingPrefix,
                "connector_host" to CommonUtils.buildConnectorUrl(request, null)
        ))
        logger.debug { "no of cards for user $hubUserMail-> ${cards.cards.count()}" }
        return ResponseEntity.ok(cards)
    }

    /**
     * REST endpoint for creating a task for an existing account
     *
     * @param authorization: Connector backend system authorization key
     * @param baseUrl: Connector base url
     * @param accountId: accountId path variable
     * @param request: CreateTaskRequest, creation of task request payload.
     * @return ResponseEntity<Any>
     */
    @PostMapping(path = ["/accounts/{accountId}/create_task"], consumes = [MediaType.APPLICATION_FORM_URLENCODED_VALUE])
    suspend fun createTaskForAccount(
            @RequestHeader(name = AUTHORIZATION) token: String,
            @RequestHeader(name = CONNECTOR_AUTH_HEADER) authorization: String,
            @RequestHeader(name = CONNECTOR_BASE_URL_HEADER) baseUrl: String,
            @PathVariable accountId: String,
            @Valid request: CreateTaskRequest): ResponseEntity<Any> {
        val hubUserMail = AuthUtil.extractUserEmail(token)
        logger.debug { "Creating Task for User $hubUserMail, accountId: $accountId, baseUrl: $baseUrl" }
        val status = service.createTaskForAccount(accountId, request, baseUrl, authorization)
        logger.debug { "Creating Task Status for $hubUserMail-> $status" }
        return ResponseEntity.status(HttpStatus.CREATED).build()
    }

    /**
     * REST endpoint for creating a appointment for an existing account
     *
     * @param authorization: Connector backend system authorization key
     * @param baseUrl: Connector base url
     * @param accountId: accountId path variable
     * @param request: CreateAppointmentRequest, creation of appointment request payload.
     * @return ResponseEntity<Any>
     */
    @PostMapping(path = ["/accounts/{accountId}/schedule_appointment"], consumes = [MediaType.APPLICATION_FORM_URLENCODED_VALUE])
    suspend fun createAppointmentForAccount(
            @RequestHeader(name = AUTHORIZATION) token: String,
            @RequestHeader(name = CONNECTOR_AUTH_HEADER) authorization: String,
            @RequestHeader(name = CONNECTOR_BASE_URL_HEADER) baseUrl: String,
            @PathVariable accountId: String,
            @Valid request: CreateAppointmentRequest): ResponseEntity<Any> {
        val hubUserMail = AuthUtil.extractUserEmail(token)
        logger.debug { "Creating Appointment for User $hubUserMail, accountId: $accountId, baseUrl: $baseUrl" }
        val status = service.createAppointmentForAccount(accountId, request, baseUrl, authorization)
        logger.debug { "Creating Appointment Status for $hubUserMail -> $status" }
        return ResponseEntity.status(HttpStatus.CREATED).build()
    }

    /**
     * REST endpoint for creating a phone call for an existing account
     *
     * @param authorization: Connector backend system authorization key
     * @param baseUrl: Connector base url
     * @param accountId: accountId path variable
     * @param request: CreatePhoneCallRequest, creation of phone call request payload.
     * @return ResponseEntity<Any>
     */
    @PostMapping(path = ["/accounts/{accountId}/schedule_phonecall"], consumes = [MediaType.APPLICATION_FORM_URLENCODED_VALUE])
    suspend fun createPhoneCallForAccount(
            @RequestHeader(name = AUTHORIZATION) token: String,
            @RequestHeader(name = CONNECTOR_AUTH_HEADER) authorization: String,
            @RequestHeader(name = CONNECTOR_BASE_URL_HEADER) baseUrl: String,
            @PathVariable accountId: String,
            @Valid request: CreatePhoneCallRequest): ResponseEntity<Any> {
        val hubUserMail = AuthUtil.extractUserEmail(token)
        logger.debug { "Scheduling Phone Call for User $hubUserMail, accountId: $accountId, baseUrl: $baseUrl" }
        val status = service.createPhoneCallForAccount(accountId, request, baseUrl, authorization)
        logger.debug { "Scheduling Phone Call Status for $hubUserMail-> $status" }
        return ResponseEntity.status(HttpStatus.CREATED).build()
    }
}
