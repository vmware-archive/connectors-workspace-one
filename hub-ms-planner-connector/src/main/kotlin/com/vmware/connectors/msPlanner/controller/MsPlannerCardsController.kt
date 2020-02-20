package com.vmware.connectors.msPlanner.controller

import com.vmware.connectors.common.payloads.response.Cards
import com.vmware.connectors.msPlanner.dto.TaskInfo
import com.vmware.connectors.msPlanner.service.MsPlannerBackendService
import com.vmware.connectors.msPlanner.utils.CardUtils
import com.vmware.connectors.msPlanner.utils.addCards
import com.vmware.connectors.msPlanner.utils.buildUserCard
import com.vmware.connectors.msPlanner.utils.getLogger
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import org.springframework.http.HttpStatus.CREATED
import org.springframework.http.MediaType.APPLICATION_FORM_URLENCODED_VALUE
import org.springframework.http.MediaType.APPLICATION_JSON_VALUE
import org.springframework.http.ResponseEntity
import org.springframework.http.server.reactive.ServerHttpRequest
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RestController
import java.util.*
import javax.validation.Valid

const val CONNECTOR_AUTH_MESSAGE_HEADER = "X-Connector-Authorization"
const val MESSAGE_ROUTING_PREFIX = "x-routing-prefix"
const val AUTHORIZATION = "Authorization"
const val CONNECTOR_BASE_URL_TEAMS_HEADER = "X-Connector-Base-Url"

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
            @RequestHeader(name = CONNECTOR_BASE_URL_TEAMS_HEADER) baseUrl: String,
            locale: Locale,
            request: ServerHttpRequest
    ): ResponseEntity<Any> {
        val userEmail = service.getUserEmailFromToken(token)
        val currentUserEmail = service.getO365UserEmailFromToken(authorization, baseUrl)

        logger.info { "Building cards for user -> $userEmail with $currentUserEmail" }

        val timeZone = service.getUserTimeZone(authorization, baseUrl) ?: "UTC"

        val tasks = service.getTasks(authorization, baseUrl)

        logger.info { "Tasks:  ${tasks.count()}" }

        val userTasks = service.getUserTasks(tasks)

        logger.info { "TasksDueToday: ${userTasks.second.count()}" }

        val userName = userTasks.first?.let { service.getUserNameFromUserId(authorization, it, baseUrl) }
        val cards = userName?.let {
            userTasks.second.map { task ->
                task.buildUserCard(request, routingPrefix, locale, cardUtils, userName, timeZone, service, authorization, baseUrl)
            }
        } ?: emptyList()

        logger.info { "cards -> ${cards.count()}" }

        return ResponseEntity.ok(Cards().addCards(cards))
    }


    /**
     * REST endpoint for adding user mentioned comment to the planner task
     *
     * @param authorization: Connector backend system authorization key
     * @param taskId: taskId path variable
     * @param request: [TaskInfo], task payload.
     * @param cardType : task card type, request param
     * @return ResponseEntity<Any>
     */
    @PostMapping(path = ["/planner/tasks/{taskId}/comment"], consumes = [APPLICATION_FORM_URLENCODED_VALUE])
    suspend fun addCommentToTask(
            @RequestHeader(name = CONNECTOR_AUTH_MESSAGE_HEADER) authorization: String,
            @RequestHeader(name = CONNECTOR_BASE_URL_TEAMS_HEADER) baseUrl: String,
            @PathVariable taskId: String,
            @Valid request: TaskInfo

    ): ResponseEntity<Any> {
        val currentUserEmail = service.getO365UserEmailFromToken(authorization, baseUrl)
        logger.info { " addCommentToTask started -> $currentUserEmail" }
        val success = service.addCommentsToTask(authorization, baseUrl, request.taskObj, request.comments)
        logger.info { " addCommentToTask status -> $success" }
        return ResponseEntity.status(CREATED).build()
    }

    /**
     * REST endpoint for adding user mentioned comment to the planner task
     *
     * @param authorization: Connector backend system authorization key
     * @param taskId: taskId path variable
     * @param request: [TaskInfo], task payload.
     * @param cardType : task card type, request param
     * @return ResponseEntity<Any>
     */
    @PostMapping(path = ["/planner/tasks/{taskId}/mark/completed"], consumes = [APPLICATION_FORM_URLENCODED_VALUE])
    suspend fun markTaskAsCompleted(
            @RequestHeader(name = CONNECTOR_AUTH_MESSAGE_HEADER) authorization: String,
            @RequestHeader(name = CONNECTOR_BASE_URL_TEAMS_HEADER) baseUrl: String,
            @PathVariable taskId: String,
            @Valid request: TaskInfo

    ): ResponseEntity<Any> {
        val currentUserEmail = service.getO365UserEmailFromToken(authorization, baseUrl)
        logger.info { "markTaskAsCompleted started -> $currentUserEmail" }
        val commentSuccess = service.addCommentsToTask(authorization, baseUrl, request.taskObj, request.comments)
        val completedSuccess = service.markTaskAsCompleted(authorization, baseUrl, request.taskObj)
        logger.info { "markTaskAsCompleted status -> $commentSuccess, $completedSuccess" }
        return ResponseEntity.status(CREATED).build()
    }

    /**
     * REST endpoint for dismissing planner tasks card

     * @param authorization: Connector backend system authorization key
     * @return ResponseEntity<Any>
     */
    @PostMapping(path = ["/planner/{cardType}/dismiss"], consumes = [APPLICATION_FORM_URLENCODED_VALUE])
    suspend fun dismissCard(
            @RequestHeader(name = CONNECTOR_AUTH_MESSAGE_HEADER) authorization: String,
            @RequestHeader(name = CONNECTOR_BASE_URL_TEAMS_HEADER) baseUrl: String
    ): ResponseEntity<Any> {
        val currentUserEmail = service.getO365UserEmailFromToken(authorization, baseUrl)
        logger.info { "Dismissed card for -> $currentUserEmail" }
        return ResponseEntity.status(HttpStatus.OK).build()
    }
}