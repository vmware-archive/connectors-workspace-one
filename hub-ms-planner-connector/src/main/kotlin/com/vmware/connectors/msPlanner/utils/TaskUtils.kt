package com.vmware.connectors.msPlanner.utils

import com.vmware.connectors.common.payloads.response.*
import com.vmware.connectors.common.utils.CommonUtils
import com.vmware.connectors.msPlanner.dto.Task
import com.vmware.connectors.msPlanner.dto.TaskDetails
import com.vmware.connectors.msPlanner.dto.UserRoles
import com.vmware.connectors.msPlanner.service.MsPlannerBackendService
import org.springframework.http.HttpMethod
import org.springframework.http.server.reactive.ServerHttpRequest
import java.net.URI
import java.util.*

private val logger = getLogger()

/**
 * Prepares UserCard
 *
 * @param request ServerHttpRequest Object
 * @param routingPrefix: Connector routing url prefix used for preparing action urls
 * @param locale: User locale while preparing cards with internationalized literals.
 * @param cardUtils cardUtils: internal module that is used while preparing cards
 * @param timeZone: timeZone of the User
 * @param service: BackendService for the MsPlanner
 * @param baseUrl: is the endPoint to be called
 * @return CardAction.Builder
 */
suspend fun Task.buildUserCard(
        request: ServerHttpRequest,
        routingPrefix: String,
        locale: Locale?,
        cardUtils: CardUtils,
        userName: String,
        timeZone: String,
        service: MsPlannerBackendService,
        authorization: String,
        baseUrl: String): Card {
    val dismissTaskBuilder = buildDismissActionBuilder(UserRoles.USER, routingPrefix, locale, cardUtils)
    val markTaskCompleteBuilder = buildMarkTaskAsCompletedActionBuilder(this, routingPrefix, locale, cardUtils)
    val addCommentBuilder = buildAddCommentToTaskActionBuilder(this, routingPrefix, locale, cardUtils)
    val showDueDate = getUserDueDateInUserTimeZone(dueDateTime!!, timeZone)
    val showStartDate = startDateTime?.let { getUserDueDateInUserTimeZone(it, timeZone) }
    val status = when (percentComplete) {
        0 -> "Not started"
        in 1..99 -> "In progress"
        100 -> "Completed"
        else -> "In progress"
    }
    val taskPriority = when (priority) {
        1 -> "Urgent"
        3 -> "Important"
        5 -> "Medium"
        9 -> "Low"
        else -> "Medium"
    }
    val latestComment = this.getLatestComment(service, authorization, baseUrl)
    val details = this.getMoreDetails(service, authorization, baseUrl)
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
                if (details != null) {
                    this.addField(cardUtils.buildGeneralBodyField("notes", details.description, locale))
                }
                if (!latestComment.isNullOrEmpty()) {
                    this.addField(cardUtils.buildGeneralBodyField("latest_comment", latestComment, locale))
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


    logger.info { "cardBodyBuilder -> $title" }

    val cardHeader = CardHeader(
            cardUtils.cardTextAccessor.getHeader(locale),
            listOf("Task due today"),
            CardHeaderLinks(
                    "",
                    listOf(url)
            )
    )
    val uniqUUID = UUID.nameUUIDFromBytes("${id}_${showDueDate}".toByteArray())
    val card = Card.Builder()
            .setId(uniqUUID)
            .setHash(uniqUUID.toString())
            .setName(cardUtils.cardTextAccessor.getMessage("card.name", locale))
            .setHeader(cardHeader)
            .setBody(cardBodyBuilder.build())
            .addAction(markTaskCompleteBuilder.build())
            .addAction(addCommentBuilder.build())
            .addAction(dismissTaskBuilder.build())

    CommonUtils.buildConnectorImageUrl(card, request)

    return card.build()
}

/**
 * Card Action Dismiss Builder
 *
 * @param task: Task object that is used for
 * @param routingPrefix: Connector routing url prefix used for preparing action urls
 * @param locale: User locale while preparing cards with internationalized literals.
 * @param cardUtils cardUtils: internal module that is used while preparing cards
 * @return CardAction.Builder
 */
private fun buildDismissActionBuilder(
        cardType: String,
        routingPrefix: String,
        locale: Locale?,
        cardUtils: CardUtils
): CardAction.Builder {

    val actionBuilder = CardAction.Builder()

    val actionLink = """/planner/$cardType/dismiss"""
    val commentsUserInputField = cardUtils.buildUserInputField("comments", "text", "actions.dismiss.inputs.comments.label", locale)

    val actionUrl = URI(routingPrefix + actionLink).normalize().toString()

    return actionBuilder
            .setLabel(cardUtils.cardTextAccessor.getActionLabel("actions.dismiss", locale))
            .setCompletedLabel(cardUtils.cardTextAccessor.getActionCompletedLabel("actions.dismiss", locale))
            .setActionKey(CardActionKey.DIRECT)
            .setUrl(actionUrl)
            .setType(HttpMethod.POST)
            .setAllowRepeated(false)
            .setMutuallyExclusiveSetId("DISMISS_TASK")
            .addUserInputField(commentsUserInputField)
            .setRemoveCardOnCompletion(true)
            .addRequestParam("actionType", "dismiss")

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
            .setPrimary(true)
            .setUrl(actionUrl)
            .setType(HttpMethod.POST)
            .setAllowRepeated(true)
            .setMutuallyExclusiveSetId("COMMENT_ON_TASK")
            .addUserInputField(commentsUserInputField)
            .setRemoveCardOnCompletion(false)
            .addRequestParam("actionType", "addComment")
            .addRequestParam("task", JsonParser.serialize(task))
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
private fun buildMarkTaskAsCompletedActionBuilder(task: Task, routingPrefix: String, locale: Locale?, cardUtils: CardUtils): CardAction.Builder {

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
            .addRequestParam("task", JsonParser.serialize(task))

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
suspend fun Task.getMoreDetails(service: MsPlannerBackendService, authorization: String, baseUrl: String): TaskDetails? {
    return try {
        service.getMoreTaskDetails(this, authorization, baseUrl)
    } catch (ex: Exception) {
        null
    }
}

/**
 * returns latestComment of the task
 *
 * @receiver Task object
 * @param service: MsPlannerBackendService
 * @param authorization is the token needed for authorizing the call
 * @param baseUrl is the endPoint to be called.
 * @returns TaskDetails object
 */
suspend fun Task.getLatestComment(service: MsPlannerBackendService, authorization: String, baseUrl: String): String? {
    return try {
        service.getLatestCommentOnTask(this, authorization, baseUrl)?.trim()
    } catch (ex: Exception) {
        null
    }
}
