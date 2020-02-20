package com.vmware.connectors.msTeams.controller


import com.vmware.connectors.common.payloads.response.Cards
import com.vmware.connectors.msTeams.dto.MessageInfo
import com.vmware.connectors.msTeams.service.BackendService
import com.vmware.connectors.msTeams.utils.*
import org.springframework.beans.factory.annotation.Autowired
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
     * Gives Card Objects that are available for the user for this connector
     *
     * @param authorization: Connector user token that is passed as a header(X-Connector-Authorization) in the request
     * @param baseUrl: Connector base url that is passed as a header(X-Connector-Base-Url) in the request
     * @param routingPrefix: Connector routing prefix, this is used if the request is proxied from a reverse-proxy/load-balancer to make the card action urls
     * @param locale: User locale
     * @param request: ServerHttpRequest
     * @return ResponseEntity<Any>: list of mentioned messages as cards for the user
     */

    @PostMapping(path = ["/cards/requests"], produces = [APPLICATION_JSON_VALUE], consumes = [APPLICATION_JSON_VALUE])
    suspend fun getCards(
            @RequestHeader(name = AUTHORIZATION) token: String,
            @RequestHeader(name = CONNECTOR_AUTH_MESSAGE_HEADER) authorization: String,
            @RequestHeader(name = MESSAGE_ROUTING_PREFIX) routingPrefix: String,
            @RequestHeader(name = CONNECTOR_BASE_URL_TEAMS_HEADER) baseUrl: String,
            locale: Locale,
            request: ServerHttpRequest
    ): ResponseEntity<Any> {
        val userEmail = service.getUserEmailFromToken(token)
        logger.info { "Building cards for user -> $userEmail" }

        val messages = service.getMentionedMessages(authorization, baseUrl)
                .distinctBy { it.id }
        val count = messages.count()
        logger.info { "no of messages-> $count" }

        val channelMessagesMap = messages.groupBy { it.channelId }
        val cards = channelMessagesMap.flatMap { (_, channelMessages) ->
            val messagesCount = channelMessages.count()
            val latestMessage = channelMessages.maxBy { it.createdDate }

            latestMessage?.let {
                listOf(latestMessage.toCard(request, messagesCount, routingPrefix, locale, cardUtils))
            } ?: emptyList()
        }
        logger.info { "cards -> $cards" }

        return ResponseEntity.ok(Cards().addCards(cards))
    }


    /**
     * REST endpoint for replying to the mentioned message
     *
     * @param authorization: Connector backend system authorization key
     * @param messageId: messageId path variable
     * @param request: [MessageInfo], message payload.
     * @return ResponseEntity<Any>
     */
    @PostMapping(path = ["/messages/{messageId}/reply"], consumes = [APPLICATION_FORM_URLENCODED_VALUE])
    suspend fun replyToMessage(
            @RequestHeader(name = CONNECTOR_AUTH_MESSAGE_HEADER) authorization: String,
            @RequestHeader(name = CONNECTOR_BASE_URL_TEAMS_HEADER) baseUrl: String,
            @PathVariable messageId: String,
            @Valid request: MessageInfo
    ): ResponseEntity<Any> {
        val message = request.messageObj
        val comments = request.comments
        if (comments != null) {
            service.replyToTeamsMessage(message, authorization, comments, baseUrl)
        }
        return ResponseEntity.status(CREATED).build()
    }


    /**
     * REST endpoint for dismissing the mentioned message

     * @param authorization: Connector backend system authorization key
     * @param messageId: messageId path variable
     * @param request: [MessageInfo], message payload.
     * @return ResponseEntity<Any>
     */
    @PostMapping(path = ["/messages/{messageId}/dismiss"], consumes = [APPLICATION_FORM_URLENCODED_VALUE])
    fun dismissMessage(
            @RequestHeader(name = CONNECTOR_AUTH_MESSAGE_HEADER) authorization: String,
            @RequestHeader(name = CONNECTOR_BASE_URL_TEAMS_HEADER) baseUrl: String,
            @PathVariable messageId: String,
            @Valid request: MessageInfo
    ): ResponseEntity<Any> {
        return ResponseEntity.status(CREATED).build()
    }

}
