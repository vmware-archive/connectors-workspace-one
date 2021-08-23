/*
* Copyright © 2020 VMware, Inc. All Rights Reserved.
* SPDX-License-Identifier: BSD-2-Clause
*/

package com.vmware.connectors.msTeams.controller


import com.vmware.connectors.common.payloads.response.Cards
import com.vmware.connectors.msTeams.config.AUTHORIZATION
import com.vmware.connectors.msTeams.config.CONNECTOR_AUTH_MESSAGE_HEADER
import com.vmware.connectors.msTeams.config.CONNECTOR_BASE_URL_HEADER
import com.vmware.connectors.msTeams.config.ROUTING_PREFIX
import com.vmware.connectors.msTeams.dto.MessageInfo
import com.vmware.connectors.msTeams.service.BackendService
import com.vmware.connectors.msTeams.utils.*
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus.OK
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
 * Cards Request and Actions Rest API Controller
 *
 * This Controller has 3 REST endpoints
 * 1. /cards/requests
 * 2. /messages/{messageId}/reply
 * 3. /messages/{messageId}/dismiss
 *
 * @property service BackendService: service to communicate with Microsoft Teams backend API
 * @property cardUtils cardUtils: internal module that is used while building cards
 * @constructor creates Rest Controller
 */
@RestController
class CardsController(
        @Autowired private val service: BackendService,
        @Autowired private val cardUtils: CardUtils
) {

    private val logger = getLogger()

    /**
     * REST endpoint for generating card objects for latest message where a user has been @mentioned for each Microsoft teams channel.
     *
     * @param token user hub token
     * @param authorization Connector user token that is passed as a header(X-Connector-Authorization) in the request
     * @param baseUrl Connector base url that is passed as a header(X-Connector-Base-Url) in the request
     * @param routingPrefix Connector routing prefix, this is used if the request is proxied from a reverse-proxy/load-balancer to make the card action urls
     * @param locale User locale
     * @param request ServerHttpRequest
     * @return ResponseEntity<Any>: list of cards for latest message where a user has been @mentioned for each Microsoft teams channel
     */

    @PostMapping(path = ["/cards/requests"], produces = [APPLICATION_JSON_VALUE], consumes = [APPLICATION_JSON_VALUE])
    suspend fun getCards(
            @RequestHeader(name = AUTHORIZATION) token: String,
            @RequestHeader(name = CONNECTOR_AUTH_MESSAGE_HEADER) authorization: String,
            @RequestHeader(name = ROUTING_PREFIX) routingPrefix: String,
            @RequestHeader(name = CONNECTOR_BASE_URL_HEADER) baseUrl: String,
            locale: Locale,
            request: ServerHttpRequest
    ): ResponseEntity<Any> {
        val userEmail = VmwareUtils.getUserEmailFromToken(token)
        logger.info { "Building cards for user -> $userEmail" }

        val mentionedMessages = service.getMentionedMessages(authorization, baseUrl)
                .distinctBy { it.id }
        val count = mentionedMessages.count()
        logger.info { "no of messages for $userEmail-> $count" }

        val channelToMessagesMap = mentionedMessages.groupBy { it.channelId }
        val cards = channelToMessagesMap.flatMap { (_, channelMessages) ->
            val messagesCount = channelMessages.count()
            val latestMessageInChannel = channelMessages.maxByOrNull { it.createdDate }

            latestMessageInChannel?.let {
                listOf(latestMessageInChannel.toCard(request, messagesCount, routingPrefix, locale, cardUtils))
            } ?: emptyList()
        }
        logger.info { "no of cards for $userEmail -> ${cards.count()}" }

        return ResponseEntity.ok(Cards().addCards(cards))
    }


    /**
     * REST endpoint for replying to the mentioned message
     *
     * @param token user hub token
     * @param authorization Connector backend system authorization key
     * @param request [MessageInfo], message payload.
     * @return ResponseEntity<Any>
     */
    @PostMapping(path = ["/messages/{messageId}/reply"], consumes = [APPLICATION_FORM_URLENCODED_VALUE])
    suspend fun replyToMessage(
            @RequestHeader(name = AUTHORIZATION) token: String,
            @RequestHeader(name = CONNECTOR_AUTH_MESSAGE_HEADER) authorization: String,
            @RequestHeader(name = CONNECTOR_BASE_URL_HEADER) baseUrl: String,
            @Valid request: MessageInfo
    ): ResponseEntity<Any> {
        println("hello")
        val userEmail = VmwareUtils.getUserEmailFromToken(token)
        val message = request.messageObj
        val comments = request.comments
        val status = if (comments != null) {
            service.replyToTeamsMessage(message, authorization, comments, baseUrl)
        } else false
        logger.info { "message replied for $userEmail -> $status" }
        return ResponseEntity.status(OK).build()
    }


    /**
     * REST endpoint for dismissing the mentioned message
     *
     * @param token user hub token
     * @return ResponseEntity<Any>
     */
    @PostMapping(path = ["/messages/{messageId}/dismiss"], consumes = [APPLICATION_FORM_URLENCODED_VALUE])
    fun dismissMessage(
            @RequestHeader(name = AUTHORIZATION) token: String
    ): ResponseEntity<Any> {
        val userEmail = VmwareUtils.getUserEmailFromToken(token)
        logger.info { "message dismissed for $userEmail -> true" }
        return ResponseEntity.status(OK).build()
    }

}
