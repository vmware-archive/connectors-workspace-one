/*
* Copyright © 2020 VMware, Inc. All Rights Reserved.
* SPDX-License-Identifier: BSD-2-Clause
*/

package com.vmware.connectors.ms.graph.utils

import com.vmware.connectors.common.payloads.response.*
import com.vmware.connectors.common.utils.CommonUtils
import com.vmware.connectors.ms.graph.config.DATE_FORMAT_PATTERN
import com.vmware.connectors.ms.graph.dto.AccessRequest
import org.springframework.http.HttpMethod
import org.springframework.http.server.reactive.ServerHttpRequest
import java.net.URI
import java.text.SimpleDateFormat
import java.util.*

private val logger = getLogger()

/**
 * Prepare Cards for accessRequests
 *
 * @param request: ServerHttpRequest object that is used for creating connector icon url
 * @param routingPrefix: Connector routing url prefix used for preparing action urls
 * @param locale: User locale while preparing cards with internationalized literals.
 * @param cardUtils: cardUtils: internal module that is used while preparing cards
 * @param userTimeZone: TimeZone of the user
 * @return List<Card>
 */
fun AccessRequest.toCard(
        request: ServerHttpRequest,
        routingPrefix: String,
        locale: Locale?,
        cardUtils: CardUtils,
        userTimeZone: String
): Card {

    val formatter = SimpleDateFormat(DATE_FORMAT_PATTERN).apply {
        this.timeZone = TimeZone.getTimeZone("UTC")
    }
    val approveForReadActionBuilder = buildApproveForReadActionBuilder(this, routingPrefix, locale, cardUtils)
    val approveForWriteActionBuilder = buildApproveForWriteActionBuilder(this, routingPrefix, locale, cardUtils)
    val declineActionBuilder = buildDeclineActionBuilder(this, routingPrefix, locale, cardUtils)
    val receivedDateString = formatter.format(receivedDate)
    val requiredDate = getDateFormatString(receivedDateString, userTimeZone)
    
    val requestedFor = requestedFor.joinToString(", ") { it.name }

    val cardBodyBuilder = CardBody.Builder()
            .apply {
                requestedBy?.let {
                    addField(cardUtils.buildGeneralBodyField("requested.by", it.name, locale))
                }
            }
            .addField(cardUtils.buildGeneralBodyField("requested.for", requestedFor, locale))
            .addField(cardUtils.buildGeneralBodyField("date", requiredDate, locale))

    val uniqUUID = UUID.nameUUIDFromBytes(id.toByteArray())

    val cardHeader = CardHeader(
            cardUtils.cardTextAccessor.getHeader(locale),
            listOf(resource.name),
            CardHeaderLinks(
                    "",
                    listOf(resource.url)
            )
    )

    logger.info { "card header setup" }

    val card = Card.Builder()
            .setId(uniqUUID)
            .setHash(uniqUUID.toString())
            .setName(cardUtils.cardTextAccessor.getMessage("card.name", locale))
            .setHeader(cardHeader)
            .setBody(cardBodyBuilder.build())
            .addAction(approveForReadActionBuilder.build())
            .addAction(approveForWriteActionBuilder.build())
            .addAction(declineActionBuilder.build())

    CommonUtils.buildConnectorImageUrl(card, request)

    return card.build()
}

/**
 * Card Action Approve For Read Builder
 *
 * @param accessRequest: AccessRequest object
 * @param routingPrefix: Connector routing url prefix used for preparing action urls
 * @param locale: User Locale
 * @return CardAction.Builder
 */
private fun buildApproveForReadActionBuilder(
        accessRequest: AccessRequest,
        routingPrefix: String,
        locale: Locale?,
        cardUtils: CardUtils
): CardAction.Builder {

    val actionBuilder = CardAction.Builder()

    val actionLink = """/access/requests/${accessRequest.id}/approve"""

    val commentsUserInputField = cardUtils.buildUserInputField("comments", "text", "actions.approve.read.inputs.comments.label", locale)

    val actionUrl = URI(routingPrefix + actionLink).normalize().toString()

    return actionBuilder
            .setLabel(cardUtils.cardTextAccessor.getActionLabel("actions.approve.read", locale))
            .setCompletedLabel(cardUtils.cardTextAccessor.getActionCompletedLabel("actions.approve.read", locale))
            .setActionKey(CardActionKey.USER_INPUT)
            .setPrimary(true)
            .setUrl(actionUrl)
            .setType(HttpMethod.POST)
            .setAllowRepeated(false)
            .setRemoveCardOnCompletion(true)
            .setMutuallyExclusiveSetId("APPROVE_ACTION")
            .addUserInputField(commentsUserInputField)
            .addRequestParam("accessRequest", JsonParser.serialize(accessRequest))
            .addRequestParam("roles", "read")
}

/**
 * Card Action Approve For Write Builder
 *
 * @param accessRequest: AccessRequest object
 * @param routingPrefix: Connector routing url prefix used for preparing action urls
 * @param locale: User Locale
 * @return CardAction.Builder
 */
private fun buildApproveForWriteActionBuilder(
        accessRequest: AccessRequest,
        routingPrefix: String,
        locale: Locale?,
        cardUtils: CardUtils
): CardAction.Builder {

    val actionBuilder = CardAction.Builder()

    val actionLink = """/access/requests/${accessRequest.id}/approve"""

    val commentsUserInputField = cardUtils.buildUserInputField("comments", "text", "actions.approve.write.inputs.comments.label", locale)

    val actionUrl = URI(routingPrefix + actionLink).normalize().toString()

    return actionBuilder
            .setLabel(cardUtils.cardTextAccessor.getActionLabel("actions.approve.write", locale))
            .setCompletedLabel(cardUtils.cardTextAccessor.getActionCompletedLabel("actions.approve.write", locale))
            .setActionKey(CardActionKey.USER_INPUT)
            .setPrimary(true)
            .setUrl(actionUrl)
            .setType(HttpMethod.POST)
            .setAllowRepeated(false)
            .setRemoveCardOnCompletion(true)
            .setMutuallyExclusiveSetId("APPROVE_ACTION")
            .addUserInputField(commentsUserInputField)
            .addRequestParam("accessRequest", JsonParser.serialize(accessRequest))
            .addRequestParam("roles", "write")
}

/**
 * Card Action Decline Builder
 *
 * @param accessRequest: AccessRequest object
 * @param routingPrefix: Connector routing url prefix used for preparing action urls
 * @param locale: User Locale
 * @return CardAction.Builder
 */
private fun buildDeclineActionBuilder(
        accessRequest: AccessRequest,
        routingPrefix: String,
        locale: Locale?,
        cardUtils: CardUtils
): CardAction.Builder {

    val actionBuilder = CardAction.Builder()

    val actionLink = """/access/requests/${accessRequest.id}/decline"""

    val commentsUserInputField = cardUtils.buildUserInputField("comments", "text", "actions.decline.inputs.comments.label", locale)

    val actionUrl = URI(routingPrefix + actionLink).normalize().toString()

    return actionBuilder
            .setLabel(cardUtils.cardTextAccessor.getActionLabel("actions.decline", locale))
            .setCompletedLabel(cardUtils.cardTextAccessor.getActionCompletedLabel("actions.decline", locale))
            .setActionKey(CardActionKey.USER_INPUT)
            .setUrl(actionUrl)
            .setType(HttpMethod.POST)
            .setAllowRepeated(false)
            .setMutuallyExclusiveSetId("APPROVE_ACTION")
            .addUserInputField(commentsUserInputField)
            .setRemoveCardOnCompletion(true)
            .addRequestParam("accessRequest", JsonParser.serialize(accessRequest))
}