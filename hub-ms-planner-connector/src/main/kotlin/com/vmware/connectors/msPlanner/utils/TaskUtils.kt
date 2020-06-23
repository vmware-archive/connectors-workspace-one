/*
* Copyright © 2020 VMware, Inc. All Rights Reserved.
* SPDX-License-Identifier: BSD-2-Clause
*/

package com.vmware.connectors.msPlanner.utils

import com.vmware.connectors.common.payloads.response.*
import com.vmware.connectors.common.utils.CommonUtils
import com.vmware.connectors.msPlanner.dto.Task
import com.vmware.connectors.msPlanner.dto.TaskDetails
import com.vmware.connectors.msPlanner.service.MsPlannerBackendService
import org.springframework.http.HttpMethod
import org.springframework.http.server.reactive.ServerHttpRequest
import java.net.URI
import java.util.*

/**
 * Prepares UserCard
 *
 * @param request ServerHttpRequest Object.
 * @param routingPrefix: Connector routing url prefix used for preparing action urls.
 * @param locale: User locale while preparing cards with internationalized literals.
 * @param cardUtils cardUtils: internal module that is used while preparing cards.
 * @param timeZone: timeZone of the User.
 * @param service: BackendService for the MsPlanner.
 * @param authorization is the token needed for authorizing the call.
 * @param baseUrl: is the endPoint to be called.
 * @return CardAction.Builder.
 */
suspend fun Task.buildUserCard(
        request: ServerHttpRequest,
        routingPrefix: String,
        locale: Locale?,
        cardUtils: CardUtils,
        timeZone: String,
        service: MsPlannerBackendService,
        authorization: String,
        baseUrl: String,
        currentUser: String?): Card {
//    val dismissTaskBuilder = buildDismissActionBuilder(routingPrefix, locale, cardUtils)
    val markTaskCompleteBuilder = buildMarkTaskAsCompletedActionBuilder(this, routingPrefix, locale, cardUtils)
    val addCommentBuilder = buildAddCommentToTaskActionBuilder(this, routingPrefix, locale, cardUtils)
    val showDueDate = getUserDueDateInUserTimeZone(dueDateTime!!, timeZone)
    val showStartDate = startDateTime?.let { getUserDueDateInUserTimeZone(it, timeZone) }
    val status = when (percentComplete) {
        0 -> "Not started"
        else -> "In progress"
    }
    val taskPriority = when (priority) {
        1 -> "Urgent"
        3 -> "Important"
        9 -> "Low"
        else -> "Medium"
    }
    val latestComment = service.getLatestCommentsOnTask(authorization, baseUrl, this, currentUser)
    val details = this.getMoreDetails(service, authorization, baseUrl, currentUser)
    val attachmentNames = details?.let {
        it.references
                .map { (key, value) -> key to value.alias }
    }
    val labels = appliedCategories.keys.map { if (categoryMap[it] == null || categoryMap[it] == "") it else categoryMap[it] }
    val cardBodyBuilder = CardBody.Builder()
            .addField(cardUtils.buildGeneralBodyField("task", title, locale))
            .also {
                if (taskMetaInfo != null) {
                    it.addField(cardUtils.buildGeneralBodyField("bucket_name", taskMetaInfo.bucketName, locale))
                    it.addField(cardUtils.buildGeneralBodyField("plan_name", taskMetaInfo.planName, locale))
                }
            }
            .addField(cardUtils.buildGeneralBodyField("status", status, locale))
            .addField(cardUtils.buildGeneralBodyField("priority", taskPriority, locale))
            .apply {
                if (details?.description != null) {
                    this.addField(cardUtils.buildGeneralBodyField("notes", details.description, locale))
                }
                if (latestComment != null) {
                    this.addField(cardUtils.buildCommentBodyField("latest_comment", latestComment, locale))
                }
                if (showStartDate != null) {
                    this.addField(cardUtils.buildGeneralBodyField("start_date", showStartDate, locale))
                }
                this.addField(cardUtils.buildGeneralBodyField("due_date", showDueDate, locale))

                if (attachmentNames != null) {
                    this.addField(cardUtils.buildAttachmentBodyField("attachments", attachmentNames, locale))
                }
                if (labels.isNotEmpty()) {
                    this.addField(cardUtils.buildGeneralBodyField("label", labels.joinToString(" , "), locale))
                }
            }


    val cardHeader = CardHeader(
            cardUtils.cardTextAccessor.getHeader(locale),
            listOf("Task due today"),
            CardHeaderLinks(
                    "",
                    listOf(url)
            )
    )
    val uniqUUID = UUID.nameUUIDFromBytes("${id}_${showDueDate}".toByteArray())
    val randId = UUID.randomUUID()
    val card = Card.Builder()
            .setId(randId)
            .setHash(randId.toString())
            .setBackendId(uniqUUID.toString())
            .setName(cardUtils.cardTextAccessor.getMessage("card.name", locale))
            .setHeader(cardHeader)
            .setBody(cardBodyBuilder.build())
            .addAction(markTaskCompleteBuilder.build())
            .addAction(addCommentBuilder.build())
//            .addAction(dismissTaskBuilder.build())

    CommonUtils.buildConnectorImageUrl(card, request)

    return card.build()
}


/**
 * Build Card Action 'Add Comment To Task'
 *
 * @param task: Task object that is used for
 * @param routingPrefix: Connector routing url prefix used for preparing action urls
 * @param locale: User locale while preparing cards with internationalized literals.
 * @param cardUtils cardUtils: internal module that is used while preparing cards
 * @return CardAction.Builder
 */
private fun buildAddCommentToTaskActionBuilder(
        task: Task,
        routingPrefix: String,
        locale: Locale?,
        cardUtils: CardUtils
): CardAction.Builder {

    val actionBuilder = CardAction.Builder()

    val actionLink = """/planner/tasks/${task.id}/comment"""
    val commentsUserInputField = cardUtils.buildUserInputField("comments", "text", "actions.addComment.inputs.comments.label", locale)

    val actionUrl = URI(routingPrefix + actionLink).normalize().toString()

    return actionBuilder
            .setLabel(cardUtils.cardTextAccessor.getActionLabel("actions.addComment", locale))
            .setCompletedLabel(cardUtils.cardTextAccessor.getActionCompletedLabel("actions.addComment", locale))
            .setActionKey(CardActionKey.USER_INPUT)
            .setPrimary(false)
            .setUrl(actionUrl)
            .setType(HttpMethod.POST)
            .setAllowRepeated(true)
            .setMutuallyExclusiveSetId("COMMENT_ON_TASK")
            .addUserInputField(commentsUserInputField)
            .setRemoveCardOnCompletion(false)
            .addRequestParam("actionType", "addComment")
            .addRequestParam("task", task.serialize())
}


/**
 * Build Card Action 'Mark Task As Completed'
 *
 * @param task: Task object that is used for
 * @param routingPrefix: Connector routing url prefix used for preparing action urls
 * @param locale: User locale while preparing cards with internationalized literals.
 * @param cardUtils cardUtils: internal module that is used while preparing cards
 * @return CardAction.Builder
 */
private fun buildMarkTaskAsCompletedActionBuilder(
        task: Task,
        routingPrefix: String,
        locale: Locale?,
        cardUtils: CardUtils): CardAction.Builder {

    val actionBuilder = CardAction.Builder()

    val actionLink = """/planner/tasks/${task.id}/mark/completed"""
    val commentsUserInputField = cardUtils.buildUserInputField("comments", "text", "actions.markAsCompleted.inputs.comments.label", locale)

    val actionUrl = URI(routingPrefix + actionLink).normalize().toString()

    return actionBuilder
            .setLabel(cardUtils.cardTextAccessor.getActionLabel("actions.markAsCompleted", locale))
            .setCompletedLabel(cardUtils.cardTextAccessor.getActionCompletedLabel("actions.markAsCompleted", locale))
            .setActionKey(CardActionKey.USER_INPUT)
            .setPrimary(true)
            .setUrl(actionUrl)
            .setType(HttpMethod.POST)
            .setAllowRepeated(false)
            .setMutuallyExclusiveSetId("COMPLETE_TASK")
            .addUserInputField(commentsUserInputField)
            .setRemoveCardOnCompletion(true)
            .addRequestParam("actionType", "markAsCompleted")
            .addRequestParam("task", task.serialize())

}

/**
 * returns details of the task
 *
 * @receiver Task object
 * @param service: MsPlannerBackendService
 * @param authorization is the token needed for authorizing the call
 * @param baseUrl is the endPoint to be called.
 * @returns TaskDetails object
 */
suspend fun Task.getMoreDetails(service: MsPlannerBackendService, authorization: String, baseUrl: String, currentUser: String?): TaskDetails? {
    return try {
        service.getMoreTaskDetails(this, authorization, baseUrl, currentUser)
    } catch (ex: Exception) {
        null
    }
}
