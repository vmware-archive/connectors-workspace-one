package com.vmware.connectors.ms.graph.controller

import com.vmware.connectors.common.payloads.response.Cards
import com.vmware.connectors.ms.graph.config.PENDING_ACCESS_REQUESTS_SINCE_HOURS
import com.vmware.connectors.ms.graph.dto.ApproveActionRequest
import com.vmware.connectors.ms.graph.service.MSGraphService
import com.vmware.connectors.ms.graph.utils.CardUtils
import com.vmware.connectors.ms.graph.utils.addCards
import com.vmware.connectors.ms.graph.utils.getLogger
import com.vmware.connectors.ms.graph.utils.toCard
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
import java.util.*
import javax.validation.Valid


const val AUTHORIZATION = "Authorization"
const val CONNECTOR_AUTH_HEADER = "X-Connector-Authorization"
const val CONNECTOR_BASE_URL_HEADER = "X-Connector-Base-Url"
const val ROUTING_PREFIX = "x-routing-prefix"

/**
 * Cards Request Rest API Controller
 *
 * This Controller has 2 REST endpoints
 * 1. /test-auth
 * 2. /cards/requests
 *
 * @property service ConnectorBackendService: service to communicate with Backend API
 * @property cardUtils cardUtils: internal module that is used while building cards
 * @constructor creates Rest Controller
 */
@RestController
class MSGraphCardsController(
        @Autowired private val service: MSGraphService,
        @Autowired private val cardUtils: CardUtils
) {

    private val logger = getLogger()

    /**
     * Gives Card Objects that are available for the user for this connector
     *
     * @param authorization: Connector user token that is passed as a header(X-Connector-Authorization) in the request
     * @param baseUrl: Connector base url that is passed as a header(X-Connector-Base-Url) in the request
     * @param routingPrefix: Connector routing prefix, this is used if the request is proxied from a reverse-proxy/load-balancer to make the card action urls
     * @param locale: User locale
     * @param request: ServerHttpRequest
     * @return ResponseEntity<Any>: list of pending access requests as cards for the user
     */
    @PostMapping(path = ["/cards/requests"], produces = [APPLICATION_JSON_VALUE], consumes = [APPLICATION_JSON_VALUE])
    suspend fun getCards(
            @RequestHeader(name = AUTHORIZATION) token: String?,
            @RequestHeader(name = CONNECTOR_BASE_URL_HEADER) baseUrl: String,
            @RequestHeader(name = CONNECTOR_AUTH_HEADER) authorization: String,
            @RequestHeader(name = ROUTING_PREFIX) routingPrefix: String,
            locale: Locale?,
            request: ServerHttpRequest
    ): ResponseEntity<Any> {

        val userEmail = service.getUserEmailFromToken(token)
        logger.info { "cards request call for : $userEmail" }

        val userTimeZone = service.getUserTimeZone(baseUrl, authorization) ?: "UTC"
        val accessRequests = service.getPendingAccessRequestsSinceMinutes(baseUrl, authorization, PENDING_ACCESS_REQUESTS_SINCE_HOURS)
                .map {
                    it.toCard(request, routingPrefix, locale, cardUtils, userTimeZone)
                }
        logger.info { "no of cards-> ${accessRequests.count()} for user $userEmail" }
        return ResponseEntity.ok(Cards().addCards(accessRequests))
    }

    /**
     * REST endpoint for approving an share access request
     *
     * @param authorization: Connector backend system authorization key
     * @param accessRequestId: accessRequestId path variable
     * @param request: [ApproveActionRequest], approve access request payload.
     * @return ResponseEntity<Any>
     */
    @PostMapping(path = ["/access/requests/{accessRequestId}/approve"], consumes = [MediaType.APPLICATION_FORM_URLENCODED_VALUE])
    suspend fun approveGrantAccessForSharingResource(
            @RequestHeader(name = CONNECTOR_BASE_URL_HEADER) baseUrl: String,
            @RequestHeader(name = CONNECTOR_AUTH_HEADER) authorization: String,
            @PathVariable accessRequestId: String,
            @Valid request: ApproveActionRequest): ResponseEntity<Any> {

        logger.info { "Approve access request id : $accessRequestId with request-> $request" }

        service.addPermissionToFile(baseUrl, request.accessRequestObj, request.rolesArray, request.comments, authorization)

        return ResponseEntity.status(HttpStatus.CREATED).build()
    }

    /**
     * REST endpoint for declining an share access request
     *
     * @param authorization: Connector backend system authorization key
     * @param baseUrl: Connector base url
     * @param accessRequestId: accessRequestId path variable
     * @param request: [ApproveActionRequest], decline access request payload.
     * @return ResponseEntity<Any>
     */
    @PostMapping(path = ["/access/requests/{accessRequestId}/decline"], consumes = [MediaType.APPLICATION_FORM_URLENCODED_VALUE])
    suspend fun declineGrantAccessForSharingResource(
            @RequestHeader(name = CONNECTOR_BASE_URL_HEADER) baseUrl: String,
            @RequestHeader(name = CONNECTOR_AUTH_HEADER) authorization: String,
            @PathVariable accessRequestId: String,
            @Valid request: ApproveActionRequest): ResponseEntity<Any> {

        logger.info { "Decline access request id : $accessRequestId with request-> $request" }

        service.declinePermissionToResource(baseUrl, request.accessRequestObj, request.comments, authorization)

        return ResponseEntity.status(HttpStatus.CREATED).build()
    }
}