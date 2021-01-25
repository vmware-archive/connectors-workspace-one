/*
* Copyright © 2020 VMware, Inc. All Rights Reserved.
* SPDX-License-Identifier: BSD-2-Clause
*/

package com.vmware.connectors.msTeams.utils

import com.vmware.connectors.common.payloads.response.*
import com.vmware.connectors.common.utils.CommonUtils
import com.vmware.connectors.msTeams.dto.Message
import org.springframework.http.HttpMethod
import org.springframework.http.server.reactive.ServerHttpRequest
import java.net.URI
import java.util.*

/**
 * Prepare Card for a Message
 *
 * @receiver Message Message object
 * @param request ServerHttpRequest object that is used for creating connector icon url
 * @param count number of times given user was @mentioned in the channel this message belongs to
 * @param routingPrefix Connector routing url prefix used for preparing action urls
 * @param locale User locale while preparing cards with internationalized literals.
 * @param cardUtils internal module that is used while preparing cards
 * @return List<Card>
 */
fun Message.toCard(
        request: ServerHttpRequest,
        count: Int,
        routingPrefix: String,
        locale: Locale?,
        cardUtils: CardUtils
): Card {
    val replyToMessageBuilder = buildReplyToMessageActionBuilder(this, routingPrefix, locale, cardUtils)
//    val dismissMessageBuilder = buildDismissActionBuilder(this, routingPrefix, locale, cardUtils)

    val paddedMessageBody = prependMentionsAndExtractBody(body.content) + " " + attachments.mapNotNull { it.getStringOrNull("name") }.joinToString(", ")
    val cardBodyBuilder = CardBody.Builder()
            .addField(cardUtils.buildGeneralBodyField("by", from.user.displayName, locale))
            .addField(cardUtils.buildGeneralBodyField("channel", channelName, locale))
            .addField(cardUtils.buildGeneralBodyField("message", paddedMessageBody, locale))
            .addField(cardUtils.buildGeneralBodyField("team", teamName, locale))
            .addField(cardUtils.buildGeneralBodyField("dateTime", createdDateInUserTimeZone, locale))


    val cardHeader = CardHeader(
            cardUtils.cardTextAccessor.getHeader(locale),
            if (count <= 1) listOf(cardUtils.cardTextAccessor.getMessage("subtitle", locale, teamName))
            else listOf(cardUtils.cardTextAccessor.getMessage("subtitle1", locale, count, teamName)),
            CardHeaderLinks(
                    "",
                    listOf(url)
            )
    )
    val uniqUUID = id.toUUID()
    val card = Card.Builder()
            .setId(uniqUUID)
            .setHash(uniqUUID.toString())
            .setName(cardUtils.cardTextAccessor.getMessage("card.name", locale))
            .setHeader(cardHeader)
            .setBody(cardBodyBuilder.build())
            .addAction(replyToMessageBuilder.build())
    card.setImageUrl("https://vmw-mf-assets.s3.amazonaws.com/connector-images/hub-ms-teams.png")

    return card.build()
}


/**
 * Reply to message action builder for a Message Card
 *
 * @param message Message object that is used for creating a reply message.
 * @param routingPrefix Connector routing url prefix used for preparing action urls
 * @param locale User locale while preparing cards with internationalized literals.
 * @param cardUtils cardUtils: internal module that is used while preparing cards
 * @return CardAction.Builder
 */
private fun buildReplyToMessageActionBuilder(
        message: Message,
        routingPrefix: String,
        locale: Locale?,
        cardUtils: CardUtils
): CardAction.Builder {

    val actionBuilder = CardAction.Builder()

    val actionLink = """/messages/${message.replyId}/reply"""

    val commentsUserInputField = cardUtils.buildUserInputField("comments", "text", "actions.replyToMessage.inputs.comments.label", locale)

    val actionUrl = URI(routingPrefix + actionLink).normalize().toString()
    val finalMessage = JsonParser.convertValue<Map<String, Any>>(message).minus("createdDate")

    return actionBuilder
            .setLabel(cardUtils.cardTextAccessor.getActionLabel("actions.replyToMessage", locale))
            .setCompletedLabel(cardUtils.cardTextAccessor.getActionCompletedLabel("actions.replyToMessage", locale))
            .setActionKey(CardActionKey.USER_INPUT)
            .setPrimary(true)
            .setUrl(actionUrl)
            .setType(HttpMethod.POST)
            .setAllowRepeated(false)
            .setRemoveCardOnCompletion(true)
            .setMutuallyExclusiveSetId("ACT_ON_MESSAGE")
            .addUserInputField(commentsUserInputField)
            .addRequestParam("message", finalMessage.serialize())
            .addRequestParam("actionType", "replyToMessage")
}

