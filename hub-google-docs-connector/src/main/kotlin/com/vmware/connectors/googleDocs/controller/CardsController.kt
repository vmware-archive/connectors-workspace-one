/*
* Copyright © 2020 VMware, Inc. All Rights Reserved.
* SPDX-License-Identifier: BSD-2-Clause
*/

package com.vmware.connectors.googleDocs.controller

import com.backflipt.commons.getLogger
import com.backflipt.commons.serialize
import com.vmware.connectors.common.utils.AuthUtil
import com.vmware.connectors.common.utils.CommonUtils
import com.vmware.connectors.googleDocs.config.*
import com.vmware.connectors.googleDocs.dto.CommentInfo
import com.vmware.connectors.googleDocs.service.BackendService
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
 * 2. /doc/{docId}/addUser
 * 3. /comment/{commentId}/reply
 *
 * @property service BackendService: service to communicate with Backend API
 * @constructor creates Rest Controller
 */
@RestController
class GoogleDocsCardsController(
        @Autowired private val service: BackendService
) {
    private val logger = getLogger()

    /**
     * REST endpoint for generating card objects
     *
     * @param token user hub token
     * @param authorization Connector user token that is passed as a header(X-Connector-Authorization) in the request
     * @param routingPrefix Connector routing prefix, this is used if the request is proxied from a reverse-proxy/load-balancer to make the card action urls
     * @param baseUrl Connector base url that is passed as a header(X-Connector-Base-Url) in the request
     * @param request ServerHttpRequest
     * @return ResponseEntity<Any>: list of cards for mentioned Comments in a Document
     */
    @PostMapping(path = ["/cards/requests"], produces = [APPLICATION_JSON_VALUE], consumes = [APPLICATION_JSON_VALUE])
    suspend fun getCards(
            @RequestHeader(name = AUTHORIZATION) token: String,
            @RequestHeader(name = CONNECTOR_AUTH_MESSAGE_HEADER) authorization: String,
            @RequestHeader(name = ROUTING_PREFIX) routingPrefix: String,
            @RequestHeader(name = CONNECTOR_BASE_URL_HEADER) baseUrl: String,
            request: ServerHttpRequest
    ): ResponseEntity<Any> {
        val userEmail = AuthUtil.extractUserEmail(token)
        logger.debug { "Building cards for user: $userEmail" }
        val comments = service
                .getMentionedComments(baseUrl, authorization, DOCUMENT_LOOKUP_WINDOW)
                .serialize()
        val cards = explang.generateCards(comments, mapOf(
                "routing_prefix" to routingPrefix,
                "connector_host" to CommonUtils.buildConnectorUrl(request, null)
        ))
        logger.debug { "No. of cards for user -> $userEmail is ${cards.cards.count()}" }
        return ResponseEntity.ok(cards)
    }

    /**
     * REST endpoint for sharing a Document to the user
     *
     * @param token user hub token
     * @param authorization Connector backend system authorization key
     * @param baseUrl Connector base url that is passed as a header(X-Connector-Base-Url) in the request
     * @param request [CommentInfo], comment payload.
     * @return ResponseEntity<Any>
     */
    @PostMapping(path = ["/doc/{docId}/addUser"], consumes = [MediaType.APPLICATION_FORM_URLENCODED_VALUE])
    suspend fun shareDocumentToUser(
            @RequestHeader(name = AUTHORIZATION) token: String,
            @RequestHeader(name = CONNECTOR_AUTH_MESSAGE_HEADER) authorization: String,
            @RequestHeader(name = CONNECTOR_BASE_URL_HEADER) baseUrl: String,
            @PathVariable docId: String,
            @Valid request: CommentInfo
    ): ResponseEntity<Any> {
        val hubUserMail = AuthUtil.extractUserEmail(token)
        val document = request.commentObj
        val email = request.comments
        logger.debug { "Document shared started for User $hubUserMail, docId: $docId, baseUrl: $baseUrl" }
        val status = if (mailValidateRegex.matches(email))
            service.inviteUserToDocument(baseUrl, authorization, document.docId, email)
        else {
            logger.error { "Entered Email by user $hubUserMail With Input $email is Not Valid" }
            throw Exception("Invalid Email")
        }
        logger.debug { "document shared status for $hubUserMail with input $email -> $status" }
        return ResponseEntity.status(HttpStatus.OK).build()
    }

    /**
     * REST endpoint for replying to the mentioned Comment
     *
     * @param token user hub token
     * @param authorization Connector backend system authorization key
     * @param baseUrl Connector base url that is passed as a header(X-Connector-Base-Url) in the request
     * @param request [CommentInfo], document payload.
     * @return ResponseEntity<Any>
     */
    @PostMapping(path = ["/comment/{commentId}/reply"], consumes = [MediaType.APPLICATION_FORM_URLENCODED_VALUE])
    suspend fun replyToComment(
            @RequestHeader(name = AUTHORIZATION) token: String,
            @RequestHeader(name = CONNECTOR_AUTH_MESSAGE_HEADER) authorization: String,
            @RequestHeader(name = CONNECTOR_BASE_URL_HEADER) baseUrl: String,
            @Valid request: CommentInfo
    ): ResponseEntity<Any> {
        val hubUserMail = AuthUtil.extractUserEmail(token)
        val comment = request.commentObj
        val docId = comment.docId
        val message = request.comments
        logger.debug { "Message reply started for User $hubUserMail, docId: $docId, baseUrl: $baseUrl" }
        val status = service.replyToMessage(baseUrl, authorization, docId, comment.commentId, message)
        logger.debug { "Message reply status for $hubUserMail -> $status" }
        return ResponseEntity.status(HttpStatus.OK).build()
    }
}
