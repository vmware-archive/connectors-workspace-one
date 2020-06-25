/*
* Copyright © 2020 VMware, Inc. All Rights Reserved.
* SPDX-License-Identifier: BSD-2-Clause
*/

package com.vmware.connectors.msPlanner.controller

import com.vmware.connectors.common.payloads.response.Cards
import com.vmware.connectors.msPlanner.config.AUTHORIZATION
import com.vmware.connectors.msPlanner.config.CONNECTOR_AUTH_MESSAGE_HEADER
import com.vmware.connectors.msPlanner.config.CONNECTOR_BASE_URL_PLANNER_HEADER
import com.vmware.connectors.msPlanner.config.MESSAGE_ROUTING_PREFIX
import com.vmware.connectors.msPlanner.dto.TaskInfo
import com.vmware.connectors.msPlanner.service.MsPlannerBackendService
import com.vmware.connectors.msPlanner.utils.*
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import org.springframework.http.HttpStatus.CREATED
import org.springframework.http.MediaType.APPLICATION_FORM_URLENCODED_VALUE
import org.springframework.http.MediaType.APPLICATION_JSON_VALUE
import org.springframework.http.ResponseEntity
import org.springframework.http.server.reactive.ServerHttpRequest
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RestController
import java.util.*
import javax.validation.Valid


/**
 * Card Actions Rest API Controller
 * It has following REST Endpoints
 * 1. /cards/requests
 * 2. /planner/tasks/{taskId}/comment
 * 3. /planner/tasks/{taskId}/mark/completed
 * 4. /planner/{cardType}/dismiss
 *
 * @property service MsPlannerBackendService: service to communicate with Microsoft Graph Planner backend APIs
 * @constructor creates Rest Controller
 */
@RestController
class CardsActionsController(
        @Autowired private val service: MsPlannerBackendService,
        @Autowired private val cardUtils: CardUtils
) {
    private val logger = getLogger()

    /**
     * REST endpoint for getting cards for planner tasks
     * @param token: Connector user token
     * @param authorization: Connector user backend token that is passed as a header(X-Connector-Authorization) in the request
     * @param routingPrefix: Connector routing prefix, this is used if the request is proxied from a
     * reverse-proxy/load-balancer to make the card action urls
     * @param baseUrl: Connector base url that is passed as a header(X-Connector-Base-Url) in the request
     * @param locale: User locale
     * @param request: ServerHttpRequest
     * @return ResponseEntity<Any>
     */
    @PostMapping(path = ["/cards/requests"], produces = [APPLICATION_JSON_VALUE], consumes = [APPLICATION_JSON_VALUE])
    suspend fun getUserPlannerTasks(
            @RequestHeader(name = AUTHORIZATION) token: String,
            @RequestHeader(name = CONNECTOR_AUTH_MESSAGE_HEADER) authorization: String,
            @RequestHeader(name = MESSAGE_ROUTING_PREFIX) routingPrefix: String,
            @RequestHeader(name = CONNECTOR_BASE_URL_PLANNER_HEADER) baseUrl: String,
            locale: Locale,
            request: ServerHttpRequest
    ): ResponseEntity<Any> {
        val currentUserEmail = service.getO365UserEmailFromToken(authorization, baseUrl)
        val response = measureTimeMillisPair {
            val userEmail = VmwareUtils.getUserEmailFromToken(token)

            logger.info { "Building cards for user -> $userEmail with $currentUserEmail" }

            val timeZone = service.getUserTimeZone(authorization, baseUrl, currentUserEmail) ?: "UTC"
            val tasks = service.getFilteredTasks(authorization, baseUrl, currentUserEmail)

            logger.info { "Tasks:  ${tasks.count()}" }

            val userTasks = service.getUserIdToAssignedTasks(tasks, timeZone)

            logger.info { "TasksDueToday: ${userTasks.second.count()}" }

            if (userTasks.first != null)
                userTasks.second.map { task ->
                    task.buildUserCard(request, routingPrefix, locale, cardUtils, timeZone, service, authorization, baseUrl, currentUserEmail)
                }
            else emptyList()
        }
        logger.info { "cards count for $currentUserEmail-> ${response.first.count()}" }
        logger.info { "total time taken for cards request for $currentUserEmail--->${response.second}" }

        return ResponseEntity.ok(Cards().addCards(response.first))
    }


    /**
     * REST endpoint for adding user mentioned comment to the planner task
     *
     * @param authorization: Connector backend system authorization key
     * @param baseUrl: Connector base url that is passed as a header(X-Connector-Base-Url) in the request
     * @param request: [TaskInfo], task payload.
     * @return ResponseEntity<Any>
     */
    @PostMapping(path = ["/planner/tasks/{taskId}/comment"], consumes = [APPLICATION_FORM_URLENCODED_VALUE])
    suspend fun addCommentToTask(
            @RequestHeader(name = CONNECTOR_AUTH_MESSAGE_HEADER) authorization: String,
            @RequestHeader(name = CONNECTOR_BASE_URL_PLANNER_HEADER) baseUrl: String,
            @Valid request: TaskInfo

    ): ResponseEntity<Any> {
        val currentUserEmail = service.getO365UserEmailFromToken(authorization, baseUrl)
        logger.info { " addCommentToTask started -> $currentUserEmail" }
        val success = service.addCommentToTask(authorization, baseUrl, request.taskObj, request.comments)
        logger.info { " addCommentToTask status -> $success" }
        return ResponseEntity.status(CREATED).build()
    }

    /**
     * REST endpoint for adding user mentioned comment to the planner task
     *
     * @param authorization: Connector backend system authorization key
     * @param baseUrl: Connector base url that is passed as a header(X-Connector-Base-Url) in the request
     * @param request: [TaskInfo], task payload.
     * @return ResponseEntity<Any>
     */
    @PostMapping(path = ["/planner/tasks/{taskId}/mark/completed"], consumes = [APPLICATION_FORM_URLENCODED_VALUE])
    suspend fun markTaskAsCompleted(
            @RequestHeader(name = CONNECTOR_AUTH_MESSAGE_HEADER) authorization: String,
            @RequestHeader(name = CONNECTOR_BASE_URL_PLANNER_HEADER) baseUrl: String,
            @Valid request: TaskInfo

    ): ResponseEntity<Any> {
        val currentUserEmail = service.getO365UserEmailFromToken(authorization, baseUrl)
        logger.info { "markTaskAsCompleted started -> $currentUserEmail" }
        val commentSuccess = service.addCommentToTask(authorization, baseUrl, request.taskObj, request.comments)
        val completedSuccess = service.markTaskAsCompleted(authorization, baseUrl, request.taskObj)
        logger.info { "markTaskAsCompleted status -> $commentSuccess, $completedSuccess" }
        return ResponseEntity.status(CREATED).build()
    }

    /**
     * REST endpoint for dismissing planner tasks card

     * @param authorization: Connector backend system authorization key
     * @param baseUrl: Connector base url that is passed as a header(X-Connector-Base-Url) in the request
     * @return ResponseEntity<Any>
     */
    @PostMapping(path = ["/planner/user/dismiss"], consumes = [APPLICATION_FORM_URLENCODED_VALUE])
    suspend fun dismissCard(
            @RequestHeader(name = CONNECTOR_AUTH_MESSAGE_HEADER) authorization: String,
            @RequestHeader(name = CONNECTOR_BASE_URL_PLANNER_HEADER) baseUrl: String
    ): ResponseEntity<Any> {
        val currentUserEmail = service.getO365UserEmailFromToken(authorization, baseUrl)
        logger.info { "Dismissed card for -> $currentUserEmail" }
        return ResponseEntity.status(HttpStatus.OK).build()
    }
}