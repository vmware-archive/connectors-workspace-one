/*
* Copyright © 2020 VMware, Inc. All Rights Reserved.
* SPDX-License-Identifier: BSD-2-Clause
*/


package com.vmware.connectors.boxNotes.controller

import com.backflipt.commons.getLogger
import com.backflipt.commons.getStringOrException
import com.backflipt.commons.serialize
import com.vmware.connectors.boxNotes.config.*
import com.vmware.connectors.boxNotes.dto.DocumentInfo
import com.vmware.connectors.boxNotes.service.BackendService
import com.vmware.connectors.common.utils.AuthUtil
import com.vmware.connectors.common.utils.CommonUtils
import com.xenovus.explang
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.http.server.reactive.ServerHttpRequest
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RestController
import javax.validation.Valid

/**
 * Cards Request and Actions Rest API Controller
 *
 * @constructor creates Rest Controller
 */
@RestController
class CardsController(
        @Autowired private val service: BackendService
) {
    private val logger = getLogger()
    /**
     * REST endpoint for generating card objects for latest comment where someone has added coments in Box Notes.
     *
     * @param token user hub token
     * @param authorization Connector user token that is passed as a header(X-Connector-Authorization) in the request
     * @param routingPrefix Connector routing prefix, this is used if the request is proxied from a reverse-proxy/load-balancer to make the card action urls
     * @param baseUrl Connector base url that is passed as a header(X-Connector-Base-Url) in the request
     * @param request ServerHttpRequest
     * @return ResponseEntity<Any>: list of cards for latest comment where a user has commented for each Box Notes.
     */
    @PostMapping(path = ["/cards/requests"], produces = [MediaType.APPLICATION_JSON_VALUE], consumes = [MediaType.APPLICATION_JSON_VALUE])
    suspend fun getCards(
            @RequestHeader(name = AUTHORIZATION) token: String,
            @RequestHeader(name = CONNECTOR_AUTH_MESSAGE_HEADER) authorization: String,
            @RequestHeader(name = ROUTING_PREFIX) routingPrefix: String,
            @RequestHeader(name = CONNECTOR_BASE_URL_HEADER) baseUrl: String,
            request: ServerHttpRequest
    ): ResponseEntity<Any> {
        val hubUserEmail = AuthUtil.extractUserEmail(token)
        val boxUserInfo = service.getUserInfo(baseUrl, authorization)
        val boxMailId = boxUserInfo.getStringOrException("login")
        logger.debug { "Building cards for user: $hubUserEmail with boxMailId: $boxMailId" }
        val boxUserName = boxUserInfo.getStringOrException("name")
        val commentsList = service.getMentionedComments(baseUrl, authorization, boxUserName)
        val cards = explang.generateCards(commentsList.serialize(), mapOf(
                "routing_prefix" to routingPrefix,
                "connector_host" to CommonUtils.buildConnectorUrl(request, null)
        ))
        logger.debug { "No of Cards For $hubUserEmail are ${cards.cards.count()}" }
        return ResponseEntity.ok(cards)
    }

    /**
     * REST endpoint for sharing a Document to the user
     *
     * @param token user hub token
     * @param authorization Connector backend system authorization key
     * @param baseUrl Connector base url that is passed as a header(X-Connector-Base-Url) in the request
     * @param request [DocumentInfo], document payload.
     * @return ResponseEntity<Any>
     */
    @PostMapping(path = ["/notes/{commentId}/addUser"], consumes = [MediaType.APPLICATION_FORM_URLENCODED_VALUE])
    suspend fun addUserToNote(
            @RequestHeader(name = AUTHORIZATION) token: String,
            @RequestHeader(name = CONNECTOR_AUTH_MESSAGE_HEADER) authorization: String,
            @RequestHeader(name = CONNECTOR_BASE_URL_HEADER) baseUrl: String,
            @Valid request: DocumentInfo
    ): ResponseEntity<Any> {
        val hubUserEmail = AuthUtil.extractUserEmail(token)
        val boxMailId = service.getUserInfo(baseUrl, authorization).getStringOrException("login")
        logger.debug { "Performing addUser Action for user: $hubUserEmail with boxMailId: $boxMailId" }
        val note = request.noteObj
        val emailId = request.comments
        val status = if (MAIL_VALIDATION_REGEX.matches(emailId)) {
            service.inviteUserToNote(baseUrl, authorization, note.noteId, emailId)
        } else {
            logger.debug { "The user entered emailId not valid $emailId" }
            throw Exception("EmailId is not valid")
        }
        logger.debug { "addUser Action status for $hubUserEmail -> $status" }
        return ResponseEntity.status(HttpStatus.OK).build()
    }

    /**
     * REST endpoint for sharing a Document to the user
     *
     * @param token user hub token
     * @param authorization Connector backend system authorization key
     * @param baseUrl Connector base url that is passed as a header(X-Connector-Base-Url) in the request
     * @param request [DocumentInfo], document payload.
     * @return ResponseEntity<Any>
     */
    @PostMapping(path = ["/notes/{commentId}/message"], consumes = [MediaType.APPLICATION_FORM_URLENCODED_VALUE])
    suspend fun addMessageToNote(
            @RequestHeader(name = AUTHORIZATION) token: String,
            @RequestHeader(name = CONNECTOR_AUTH_MESSAGE_HEADER) authorization: String,
            @RequestHeader(name = CONNECTOR_BASE_URL_HEADER) baseUrl: String,
            @Valid request: DocumentInfo
    ): ResponseEntity<Any> {
        val userEmail = AuthUtil.extractUserEmail(token)
        val boxMailId = service.getUserInfo(baseUrl, authorization)
                .getStringOrException("login")
        logger.debug { "Performing add message for user -> $userEmail with boxMailId->$boxMailId" }
        val note = request.noteObj
        val comments = request.comments
        val status = service.addComment(baseUrl, authorization, note.noteId, comments)
        logger.debug { "Add message status for $userEmail -> $status" }
        return ResponseEntity.status(HttpStatus.OK).build()
    }
}
