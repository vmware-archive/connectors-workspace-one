/*
* Copyright © 2020 VMware, Inc. All Rights Reserved.
* SPDX-License-Identifier: BSD-2-Clause
*/

package com.vmware.connectors.sfLeads.controller

import com.backflipt.commons.getLogger
import com.backflipt.commons.serialize
import com.vmware.connectors.common.utils.AuthUtil
import com.vmware.connectors.common.utils.CommonUtils
import com.vmware.connectors.sfLeads.config.AUTHORIZATION
import com.vmware.connectors.sfLeads.config.CONNECTOR_AUTH_HEADER
import com.vmware.connectors.sfLeads.config.CONNECTOR_BASE_URL_HEADER
import com.vmware.connectors.sfLeads.config.ROUTING_PREFIX
import com.vmware.connectors.sfLeads.dto.LeadInfo
import com.vmware.connectors.sfLeads.service.BackendService
import com.xenovus.explang
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
 * This Controller has 3 REST endpoints
 * 1. /cards/requests
 * 2. /leads/{leadId}/addTask
 * 3. /leads/{leadId}/logACall
 *
 * @property service BackendService: service to communicate with Backend API
 * @constructor creates Rest Controller
 */
@RestController
class CardsController(
        @Autowired private val service: BackendService
) {
    private val logger = getLogger()

    /**
     * Gives Card Objects that are available for the user for this connector
     *
     * @param token: Connector user token
     * @param authorization: Connector user token that is passed as a header(X-Connector-Authorization) in the request
     * @param baseUrl: Connector base url that is passed as a header(X-Connector-Base-Url) in the request
     * @param routingPrefix: Connector routing prefix, this is used if the request is proxied from a reverse-proxy/load-balancer to make the card action urls
     * @param request: ServerHttpRequest
     * @return ResponseEntity<Any>: list of newly created accounts as cards for the user
     */
    @PostMapping(path = ["/cards/requests"], produces = [APPLICATION_JSON_VALUE], consumes = [APPLICATION_JSON_VALUE])
    suspend fun getCards(
            @RequestHeader(name = AUTHORIZATION) token: String,
            @RequestHeader(name = CONNECTOR_AUTH_HEADER) authorization: String,
            @RequestHeader(name = CONNECTOR_BASE_URL_HEADER) baseUrl: String,
            @RequestHeader(name = ROUTING_PREFIX) routingPrefix: String,
            request: ServerHttpRequest
    ): ResponseEntity<Any> {
        val hubUserEmail = AuthUtil.extractUserEmail(token)
        logger.debug { "Building cards for user -> $hubUserEmail" }
        val leads = service
                .getRecentLeads(baseUrl, authorization)
                .also {
                    logger.debug { "No. of Assigned Leads For $hubUserEmail is ${it.size}" }
                }
                .serialize()
        val cards = explang.generateCards(leads, mapOf(
                "routing_prefix" to routingPrefix,
                "connector_host" to CommonUtils.buildConnectorUrl(request, null)
        ))
        logger.debug { "No. of Cards For $hubUserEmail is ${cards.cards.count()}" }
        return ResponseEntity.ok(cards)
    }

    /**
     * REST endpoint for adding a Task for existing Lead
     *
     * @param token: Connector user token
     * @param authorization Connector user token that is passed as a header(X-Connector-Authorization) in the request
     * @param baseUrl: Connector base url that is passed as a header(X-Connector-Base-Url) in the request
     * @param leadId:  id of lead
     * @param request [LeadInfo], LeadInfo payload.
     * @return ResponseEntity<Any>
     */
    @PostMapping(path = ["/leads/{leadId}/addTask"], consumes = [MediaType.APPLICATION_FORM_URLENCODED_VALUE])
    suspend fun addTaskToLead(
            @RequestHeader(name = AUTHORIZATION) token: String,
            @RequestHeader(name = CONNECTOR_AUTH_HEADER) authorization: String,
            @RequestHeader(name = CONNECTOR_BASE_URL_HEADER) baseUrl: String,
            @PathVariable leadId: String,
            @Valid request: LeadInfo
    ): ResponseEntity<Any> {
        val hubUserMail = AuthUtil.extractUserEmail(token)
        logger.debug { "Add task for user $hubUserMail, leadId: $leadId, baseUrl: $baseUrl" }
        val status = service.createTaskForLead(baseUrl, authorization, leadId, request.comments)
        logger.debug { "Add task status for user $hubUserMail:  $status" }
        return ResponseEntity.status(HttpStatus.OK).build()
    }

    /**
     * REST endpoint for logging A Call for an existing Lead
     *
     * @param token: Connector user token
     * @param authorization Connector user token that is passed as a header(X-Connector-Authorization) in the request
     * @param baseUrl: Connector base url that is passed as a header(X-Connector-Base-Url) in the request
     * @param leadId:  id of lead
     * @param request [LeadInfo], LeadInfo payload.
     * @return ResponseEntity<Any>
     */
    @PostMapping(path = ["/leads/{leadId}/logACall"], consumes = [MediaType.APPLICATION_FORM_URLENCODED_VALUE])
    suspend fun logACallToLead(
            @RequestHeader(name = AUTHORIZATION) token: String,
            @RequestHeader(name = CONNECTOR_AUTH_HEADER) authorization: String,
            @RequestHeader(name = CONNECTOR_BASE_URL_HEADER) baseUrl: String,
            @PathVariable leadId: String,
            @Valid request: LeadInfo
    ): ResponseEntity<Any> {
        val hubUserMail = AuthUtil.extractUserEmail(token)
        logger.debug { "Log a call for user $hubUserMail, leadId: $leadId, baseUrl: $baseUrl" }
        val comments = request.comments
        val status = service.logACall(baseUrl, authorization, leadId, comments)
        logger.debug { "Log a call status for $hubUserMail: $status" }
        return ResponseEntity.status(HttpStatus.OK).build()
    }
}