/*
* Copyright © 2020 VMware, Inc. All Rights Reserved.
* SPDX-License-Identifier: BSD-2-Clause
*/

package com.vmware.connectors.concur.controller

import com.backflipt.commons.getLogger
import com.backflipt.commons.measureTimeMillisPair
import com.backflipt.commons.serialize
import com.vmware.connectors.common.utils.AuthUtil
import com.vmware.connectors.common.utils.CommonUtils
import com.vmware.connectors.concur.config.AUTHORIZATION
import com.vmware.connectors.concur.config.CONNECTOR_AUTH_HEADER
import com.vmware.connectors.concur.config.CONNECTOR_BASE_URL_HEADER
import com.vmware.connectors.concur.config.ROUTING_PREFIX
import com.vmware.connectors.concur.dto.TravelRequestInfo
import com.vmware.connectors.concur.service.BackendService
import com.xenovus.explang.generateCards
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.MediaType.APPLICATION_JSON_VALUE
import org.springframework.http.ResponseEntity
import org.springframework.http.server.reactive.ServerHttpRequest
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RestController
import javax.validation.Valid

/**
 * Cards Request Rest API Controller
 *
 * This Controller has 3 REST endpoints
 * 1. /cards/requests
 * 2./travelrequest/workflowaction
 * 3./card/dismiss
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
     * @param token: Connector token that is passed as a header(authorization) in the request
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
        val hubEmailId = AuthUtil.extractUserEmail(token)
        val userLoginId = service.getUserLoginId(hubEmailId, baseUrl, authorization)
        val (cards, time) = measureTimeMillisPair {
            logger.debug { "Received cards request for hubEmailId -> $hubEmailId, concurLoginId -> $userLoginId" }
            val travelRequests = service
                    .getApprovableTravelRequests(userLoginId, baseUrl, authorization)
                    .also {
                        logger.debug { "No. of pending Travel requests for approval for hubEmailId -> $hubEmailId, concurLoginId -> $userLoginId: ${it.size}" }
                    }
                    .serialize()
            logger.debug { "Travel requests input data to engine: $travelRequests" }
            val cards = generateCards(travelRequests, mapOf(
                    "routing_prefix" to routingPrefix,
                    "connector_host" to CommonUtils.buildConnectorUrl(request, null)
            ))
            logger.debug { "No. of cards for hubEmailId -> $hubEmailId, concurLoginId -> $userLoginId: ${cards.cards.count()}" }
            logger.debug { "Cards response payload: ${cards.serialize()}" }
            cards
        }
        logger.debug { "Total time taken for cards request for user: $hubEmailId: $time ms" }
        return ResponseEntity.ok(cards)
    }

    /**
     * REST endpoint for performing action on travel request
     *
     * @param token: Connector token that is passed as a header(authorization) in the request
     * @param authorization: Connector user token that is passed as a header(X-Connector-Authorization) in the request
     * @param baseUrl: Connector base url that is passed as a header(X-Connector-Base-Url) in the request
     * @param request: [TravelRequestInfo], travel request payload.
     * @return ResponseEntity<Any>
     */
    @PostMapping(path = ["/travelrequest/workflowaction"], consumes = [MediaType.APPLICATION_FORM_URLENCODED_VALUE])
    suspend fun performAction(
            @RequestHeader(name = AUTHORIZATION) token: String,
            @RequestHeader(name = CONNECTOR_AUTH_HEADER) authorization: String,
            @RequestHeader(name = CONNECTOR_BASE_URL_HEADER) baseUrl: String,
            @Valid request: TravelRequestInfo,
            httpRequest: ServerHttpRequest
    ): ResponseEntity<Any> {
        val hubEmailId = AuthUtil.extractUserEmail(token)
        val (_, time) = measureTimeMillisPair {
            val userLoginId = service.getUserLoginId(hubEmailId, baseUrl, authorization)
            logger.debug { "REQUEST HEADERS: ${httpRequest.headers}" }
            logger.debug { "Performing action: ${request.actionType} for hubEmailId -> $hubEmailId, concurLoginId -> $userLoginId" }
            val status = service.performAction(
                    userLoginId,
                    authorization,
                    request.workflowActionURL,
                    request.actionType,
                    request.comments
            )
            logger.debug { "Action: ${request.actionType} successful --> $status" }
        }
        logger.debug { "Total time taken for action: ${request.actionType} request for hubEmailId -> $hubEmailId: $time ms" }
        return ResponseEntity.status(HttpStatus.OK).build()
    }
}
