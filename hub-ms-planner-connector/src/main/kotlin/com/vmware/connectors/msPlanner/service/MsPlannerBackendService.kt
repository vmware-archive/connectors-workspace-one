/*
* Copyright © 2020 VMware, Inc. All Rights Reserved.
* SPDX-License-Identifier: BSD-2-Clause
*/

package com.vmware.connectors.msPlanner.service

import com.vmware.connectors.msPlanner.config.Endpoints
import com.vmware.connectors.msPlanner.config.Endpoints.getGroupIdsUrl
import com.vmware.connectors.msPlanner.config.Endpoints.getNewConversationThreadUrl
import com.vmware.connectors.msPlanner.config.Endpoints.replyToTaskUrl
import com.vmware.connectors.msPlanner.config.FORMATTER
import com.vmware.connectors.msPlanner.config.MAX_DUE_DAYS
import com.vmware.connectors.msPlanner.dto.*
import com.vmware.connectors.msPlanner.utils.*
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.*
import reactor.core.publisher.Mono
import java.util.*

/**
 * MsPlannerBackendService class contains functions which will fetch the tasks
 * that are due Today and the following Actions(Mark Task as Completed,Add Comment to Task)
 * and every function in the class requires the following parameters
 * baseUrl - tenant url
 * authorization - Microsoft Teams User Bearer Token
 *
 * @property client: WebClient: library to make async http calls
 */
@Component
class MsPlannerBackendService(@Autowired private val client: WebClient) {

    private val logger = getLogger()

    /**
     * fetches the user id of the current User.
     *
     * @param authorization is the token needed for authorizing the call.
     * @param baseUrl is the endPoint to be called.
     * @returns the user's id.
     */
    private suspend fun getUserId(
            authorization: String,
            baseUrl: String,
            currentUser: String?
    ): String {
        val resp = measureTimeMillisPair {
            client
                    .get()
                    .uri(Endpoints.getUserIdUrl(baseUrl))
                    .header(HttpHeaders.AUTHORIZATION, authorization)
                    .awaitExchangeAndThrowError()
                    .awaitBody<Map<String, Any>>()
                    .getStringOrDefault("id")
        }
        logger.debug { "Time Taken For getUserId for $currentUser->${resp.second}" }
        return resp.first
    }

    /**
     * fetches the user's TimeZone or
     * Null if the user did not set TimeZone.
     *
     * @param authorization is the token needed for authorizing the call.
     * @param baseUrl is the endPoint to be called.
     * @returns the timeZone of the User.
     */
    suspend fun getUserTimeZone(
            authorization: String,
            baseUrl: String,
            currentUser: String?
    ): String? {
        val url = Endpoints.getUserTimeZoneUrl(baseUrl)
        val response = measureTimeMillisPair {
            val status = client
                    .get()
                    .uri(url)
                    .header(HttpHeaders.AUTHORIZATION, authorization)
                    .awaitExchangeAndThrowError {
                        logger.error(it) { "Error While Fetching Time Zone For User $currentUser" }
                    }
            if (status.statusCode() == HttpStatus.NO_CONTENT)
                null
            else
                status.awaitBody<Map<String, Any>>()
                        .getStringOrNull("value")
        }
        logger.debug { "Time Taken For getUserTimeZone for $currentUser-->${response.second}" }
        return response.first
    }

    /**
     * fetches the user groupIds.
     *
     * @param authorization is the token needed for authorizing the call.
     * @param baseUrl is the endPoint to be called.
     * @returns the list of group Ids.
     */
    private suspend fun getGroupIds(
            authorization: String,
            baseUrl: String,
            currentUser: String?
    ): List<String> {
        val response = measureTimeMillisPair {
            val url = getGroupIdsUrl(baseUrl)
            client
                    .post()
                    .uri(url)
                    .header(HttpHeaders.AUTHORIZATION, authorization)
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                    .body(
                            Mono.just(
                                    mapOf(
                                            "securityEnabledOnly" to false
                                    )
                            )
                    )
                    .awaitExchangeAndThrowError {
                        logger.error(it) { "Error While Fetching GroupIds For User $currentUser Url:$url" }
                    }
                    .awaitBody<Map<String, Any>>()
        }
        logger.debug { "Time Taken to getGroupIds for $currentUser--->${response.second}" }
        return response.first.getListOrException("value")
    }

    /**
     * fetches the Information About Plans
     *
     * @param authorization is the token needed for authorizing the call.
     * @param baseUrl is the endPoint to be called.
     * @param groupIds is the list of group Ids.
     * @param plans is the list of Plans to be returned.
     * @returns the list of Plans with Details.
     */
    private suspend fun getPlanInfoFromGroupIds(
            authorization: String,
            baseUrl: String,
            groupIds: List<String>,
            currentUser: String?,
            plans: List<String> = emptyList()
    ): List<String> {
        val response = measureTimeMillisPair {
            when {
                groupIds.isEmpty() -> plans
                groupIds.size <= 20 ->
                    plans.plus(
                            getBucketsOrTasksInfoFromBatchBody(authorization, baseUrl, buildPlanBatchBody(groupIds), currentUser)
                                    .map {
                                        val planId = it.getStringOrException("id")
                                        val groupId = it.getStringOrException("groupId")
                                        val title = it.getStringOrException("title")
                                        "$groupId $planId $title"
                                    }
                    )
                else -> {
                    val batchBody = buildPlanBatchBody(groupIds.take(20))
                    val totalPlans = plans.plus(getBucketsOrTasksInfoFromBatchBody(authorization, baseUrl, batchBody, currentUser)
                            .map {
                                val planId = it.getStringOrException("id")
                                val groupId = it.getStringOrException("groupId")
                                val title = it.getStringOrException("title")
                                "$groupId $planId $title"
                            })
                    getPlanInfoFromGroupIds(authorization, baseUrl, groupIds.drop(20), currentUser, totalPlans)
                }
            }
        }
        logger.debug { "Time Taken to get All Plans for $currentUser--->${response.second}" }
        return response.first
    }

    /**
     * fetches the Information About Buckets
     *
     * @param authorization is the token needed for authorizing the call.
     * @param baseUrl is the endPoint to be called.
     * @param planIds is the list of Planner Ids.
     * @param buckets is the list of buckets to be returned.
     * @returns the list of Buckets With Details.
     */
    private suspend fun getBucketInfoFromPlanIds(
            authorization: String,
            baseUrl: String,
            currentUser: String?,
            planIds: List<String>,
            buckets: List<String> = emptyList()
    ): List<String> {
        val response = measureTimeMillisPair {
            when {
                planIds.isEmpty() -> buckets
                planIds.size <= 20 ->
                    buckets.plus(getBucketsOrTasksInfoFromBatchBody(authorization, baseUrl, buildBucketBatchBody(planIds), currentUser, isBucket = true)
                            .map {
                                val bucketId = it.getStringOrException("id")
                                val groupId = it.getStringOrException("groupId")
                                val planName = it.getStringOrException("title")
                                val bucketName = it.getStringOrException("name")
                                TaskMetaInfo(groupId, bucketId, planName, bucketName).serialize()
                            })
                else -> {
                    val batchBody = buildBucketBatchBody(planIds.take(20))
                    val totalBuckets = buckets.plus(getBucketsOrTasksInfoFromBatchBody(authorization, baseUrl, batchBody, currentUser, isBucket = true)
                            .map {
                                val bucketId = it.getStringOrException("id")
                                val groupId = it.getStringOrException("groupId")
                                val planName = it.getStringOrException("title")
                                val bucketName = it.getStringOrException("name")
                                TaskMetaInfo(groupId, bucketId, planName, bucketName).serialize()
                            })
                    getBucketInfoFromPlanIds(authorization, baseUrl, currentUser, planIds.drop(20), totalBuckets)
                }
            }
        }
        logger.debug { "Time Taken to get All Buckets for $currentUser--->${response.second}" }
        return response.first
    }

    /**
     * fetches the Tasks that are DueToday.
     *
     * @param authorization is the token needed for authorizing the call.
     * @param baseUrl is the endPoint to be called.
     * @returns the list of filtered Tasks.
     */
    suspend fun getFilteredTasks(
            authorization: String,
            baseUrl: String,
            currentUser: String?
    ): List<Task> {
        val response = measureTimeMillisPair {
            val userId = getUserId(authorization, baseUrl, currentUser)
            val groupIds = getGroupIds(authorization, baseUrl, currentUser)
            val planIds = getPlanInfoFromGroupIds(authorization, baseUrl, groupIds, currentUser)
            val planCategoriesMap = planIds.map { it.split(" ")[1] }.associateWith { getLabelCategoriesForPlan(authorization, baseUrl, it, currentUser) }
            val bucketIds = getBucketInfoFromPlanIds(authorization, baseUrl, currentUser, planIds)
            val tasks = getTasks(authorization, baseUrl, bucketIds, currentUser).map {
                val planId = it.getStringOrException("planId")
                it.plus("userId" to userId)
                        .plus("categoryMap" to planCategoriesMap.getValue(planId))
            }
            tasks.map { it.convertValue<Task>() }
                    .filter {
                        it.percentComplete != 100 && it.dueDateTime != null
                                && it.dueDateTime <= formatDateToString(Date().plus(Days(MAX_DUE_DAYS, "UTC")))
                    }
        }
        logger.debug { "time taken for getting tasks due today for $currentUser-->${response.second}" }
        return response.first
    }

    /**
     * fetches the Batch response
     * when [isBucket] is false this will fetch the Task Batch Response
     * when [isTask] is false this will fetch the Bucket Batch Response
     *
     * @param authorization is the token needed for authorizing the call.
     * @param baseUrl is the endPoint to be called.
     * @param batchBody is the body of the batch request.
     * @param isBucket whether the batch body Belongs to Bucket or Not.
     * @param isTask whether the batch body Belongs to Task or Not.
     * @returns the list of Map that contains the Batch Response.
     */
    private suspend fun getBucketsOrTasksInfoFromBatchBody(
            authorization: String,
            baseUrl: String,
            batchBody: List<Map<String, String>>,
            currentUser: String?,
            isBucket: Boolean = false,
            isTask: Boolean = false
    ): List<Map<String, Any>> {
        val response = measureTimeMillisPair {
            client
                    .post()
                    .uri(Endpoints.getAllIdsUrl(baseUrl))
                    .header(HttpHeaders.AUTHORIZATION, authorization)
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                    .header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                    .body(
                            Mono.just(
                                    mapOf(
                                            "requests" to batchBody
                                    )
                            )
                    )
                    .awaitExchangeAndThrowError()
                    .awaitBody<Map<String, Any>>()
        }
        val responses = response.first.getListOrException<Map<String, Any>>("responses")
        val body = responses
                .map { (it.getMapOrException<String, Any>("body")).plus("id" to it.getStringOrException("id")) }
        logger.debug { "Time Taken to get Ids Common Function for $currentUser--->${response.second}" }
        return when {
            isBucket -> body.mapNotNull {
                val id = it.getStringOrException("id").split(" ")
                val groupId = id.first()
                val title = id.drop(2).joinToString(separator = " ")
                it.getListOrNull<Map<String, Any>>("value")?.map { value ->
                    value.plus(mapOf("groupId" to groupId, "title" to title))
                }
            }.flatten()
            isTask -> body.mapNotNull {
                it.getListOrNull<Map<String, Any>>("value")
            }.flatten()
            else -> body.mapNotNull {
                it.getListOrNull<Map<String, Any>>("value")?.map { value ->
                    value.plus(mapOf("groupId" to it.getValue("id")))
                }
            }.flatten()
        }
    }

    /**
     *  fetch all the user's tasks
     *
     * @param authorization is the token needed for authorizing the call.
     * @param baseUrl is the endPoint to be called.
     * @param bucketIds is the List of User's Bucket Ids.
     * @param tasks is List of Map that contains Task Response.
     * @returns the list of all user's tasks.
     */
    private suspend fun getTasks(
            authorization: String,
            baseUrl: String,
            bucketIds: List<String>,
            currentUser: String?,
            tasks: List<Map<String, Any>> = emptyList()
    ): List<Map<String, Any>> {
        val bucketIdsMap = bucketIds.associate {
            val taskMetaInfo = it.deserialize<TaskMetaInfo>()
            taskMetaInfo.bucketId to taskMetaInfo
        }
        return when {
            bucketIds.isEmpty() -> tasks
            bucketIds.size <= 20 ->
                tasks.plus(getBucketsOrTasksInfoFromBatchBody(authorization, baseUrl, buildTaskBatchBody(bucketIds), currentUser, isBucket = false, isTask = true).map {
                    it.plus("taskMetaInfo" to bucketIdsMap.getValue(it.getStringOrException("bucketId")))
                })
            else -> {
                val batchBody = buildTaskBatchBody(bucketIds.take(20))
                val totalTasks = tasks.plus(getBucketsOrTasksInfoFromBatchBody(authorization, baseUrl, batchBody, currentUser).map {
                    it.plus("taskMetaInfo" to bucketIdsMap.getValue(it.getStringOrException("bucketId")))
                })
                getTasks(authorization, baseUrl, bucketIds.drop(20), currentUser, totalTasks)
            }
        }
    }

    /**
     * fetches the userid and assigned tasks for the user
     * @param tasks list of user's tasks.
     * @param timeZone timeZon of the User.
     * @returns the pair of userId to Assigned tasks.
     */
    fun getUserIdToAssignedTasks(
            tasks: List<Task>,
            timeZone: String
    ): Pair<String?, List<Task>> {
        val filteredTasks = tasks
                .filter {
                    it.assignments.keys.contains(it.userId) && it.dueDateTime != null &&
                            getUserDueDateInUserTimeZone(it.dueDateTime, timeZone, FORMATTER) ==
                            getUserDueDateInUserTimeZone(getCurrentUtcTime(), timeZone, FORMATTER)
                }
                .sortedBy { it.dueDateTime }
        val userId = tasks.firstOrNull()?.userId
        return userId to filteredTasks
    }

    /**
     * updates the task as completed.
     * @param authorization is the token needed for authorizing the call.
     * @param baseUrl is the endPoint to be called.
     * @param task is the Task Object.
     * @returns the status of the Completion of Task.
     */
    suspend fun markTaskAsCompleted(
            authorization: String,
            baseUrl: String,
            task: Task
    ): Boolean {
        val body = mapOf("percentComplete" to 100)
        return updateTask(authorization, baseUrl, task, body)
    }

    /**
     * creates a new conversation thread and add the comment to the task
     *
     * @param authorization is the token needed for authorizing the call.
     * @param baseUrl is the endPoint to be called.
     * @param task is the Task Object.
     * @param comment is the first message to be posted.
     * @returns the status of the update.
     */
    private suspend fun updateConversationId(
            authorization: String,
            baseUrl: String,
            task: Task,
            comment: String
    ): Boolean {
        val body = mapOf("conversationThreadId" to
                createNewConversationThread(
                        authorization, baseUrl, task.groupId, task.title, comment
                ))
        return updateTask(authorization, baseUrl, task, body)
    }

    /**
     * updates the task Object.
     * @param authorization is the token needed for authorizing the call.
     * @param baseUrl is the endPoint to be called.
     * @param task is the Task Object to be updated.
     * @param body of the post request to update the Task.
     * @returns the status of the Update.
     */
    private suspend fun updateTask(
            authorization: String,
            baseUrl: String,
            task: Task,
            body: Map<String, Any>
    ): Boolean {
        val eTag = task.eTag
        val url = Endpoints.updateTaskUrl(baseUrl, task.id)
        val response = measureTimeMillisPair {
            client
                    .patch()
                    .uri(url)
                    .header(HttpHeaders.AUTHORIZATION, authorization)
                    .header(HttpHeaders.IF_MATCH, eTag)
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                    .body(Mono.just(body))
                    .awaitExchangeAndThrowError {
                        logger.error(it) { "Error while Updating the Task Url:$url Task Object:${task.serialize()}" }
                    }
        }
        logger.debug { "time taken to update Task---->${response.second}" }
        return response.first
                .statusCode()
                .is2xxSuccessful
    }

    /**
     *
     * will add the comment to the task.
     * @param authorization is the token needed for authorizing the call.
     * @param baseUrl is the endPoint to be called.
     * @param task is the Task Object to be updated.
     * @param message text to be Added to the task.
     * @return if comment is successfully added true else false
     */
    suspend fun addCommentToTask(
            authorization: String,
            baseUrl: String,
            task: Task,
            message: String?
    ): Boolean {
        return if (task.conversationThreadId != null && message != null) {
            val url = replyToTaskUrl(baseUrl, task.groupId, task.conversationThreadId)
            client
                    .post()
                    .uri(url)
                    .header(HttpHeaders.AUTHORIZATION, authorization)
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                    .body(Mono.just(buildAddCommentsToTaskBody(message)))
                    .awaitExchangeAndThrowError {
                        logger.error(it) { "Error while Adding Comment to the Task Url:$url Task Object:${task.serialize()}" }
                    }
                    .statusCode()
                    .is2xxSuccessful
        } else {
            if (message != null) {
                val updatedTask = getTaskById(baseUrl, authorization, task.id, task.groupId)
                if (updatedTask.conversationThreadId == null) {
                    updateConversationId(authorization, baseUrl, task, message)
                } else addCommentToTask(authorization, baseUrl, updatedTask, message)
            } else false
        }
    }

    /**
     * creates a conversation and fetches the New ConversationThread Id.
     * @param authorization is the token needed for authorizing the call.
     * @param baseUrl is the endPoint to be called.
     * @param groupId is the user's Group Id.
     * @param taskName is the Name of the Task.
     * @param comment text to be Added to the task.
     * @return New ConversationThread Id.
     */
    private suspend fun createNewConversationThread(
            authorization: String,
            baseUrl: String,
            groupId: String,
            taskName: String,
            comment: String
    ): String {
        val url = getNewConversationThreadUrl(baseUrl, groupId)
        val response = measureTimeMillisPair {
            client
                    .post()
                    .uri(url)
                    .header(HttpHeaders.AUTHORIZATION, authorization)
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                    .body(Mono.just(buildConversationThreadBody(taskName, comment)))
                    .awaitExchangeAndThrowError()
                    .awaitBody<Map<String, Any>>()
                    .getListOrException<Map<String, Any>>("threads").first()
                    .getStringOrException("id")
        }
        logger.debug { "time Taken For getNewConversationThread---->${response.second}" }
        return response.first
    }

    /**
     * fetches the More Task Details Using the [taskId].
     *
     * @param baseUrl is the endPoint to be called.
     * @param authorization is the token needed for authorizing the call.
     * @param taskId is the Id of the Task.
     * @param groupId is the Id of the Group to which the task belongs to.
     * @return Task Object.
     */
    private suspend fun getTaskById(
            baseUrl: String,
            authorization: String,
            taskId: String,
            groupId: String): Task {
        val url = Endpoints.getTaskByIdUrl(baseUrl, taskId)
        val response = measureTimeMillisPair {
            client.get()
                    .uri(url)
                    .header(HttpHeaders.AUTHORIZATION, authorization)
                    .awaitExchangeAndThrowError()
                    .awaitBody<Map<String, Any>>()
                    .plus(mapOf("groupId" to groupId))
        }
        logger.debug { "time taken for getTaskById---->${response.second}" }
        return response.first.convertValue()
    }

    /**
     * get userPrincipalName(email) from the user office365 profile.
     * @param authorization: authorization OAuth token.
     * @param baseUrl is the endPoint to be called.
     * @returns user emailId.
     */

    suspend fun getO365UserEmailFromToken(authorization: String, baseUrl: String): String? {
        val url = Endpoints.getUserIdUrl(baseUrl)
        return client
                .get()
                .uri(url)
                .header(HttpHeaders.AUTHORIZATION, authorization)
                .awaitExchangeAndThrowError {
                    logger.error(it) { "error while getting 0365 EmailToken" }
                }
                .awaitBody<Map<String, Any>>()
                .getStringOrNull("userPrincipalName")
    }

    /**
     * fetches detailed Information About the Task.
     *
     * @param authorization is the token needed for authorizing the call.
     * @param baseUrl is the endPoint to be called.
     * @param task Task Object.
     * @returns TaskDetails object.
     */
    suspend fun getMoreTaskDetails(task: Task, authorization: String, baseUrl: String, currentUser: String?): TaskDetails {
        val response = measureTimeMillisPair {
            client
                    .get()
                    .uri(Endpoints.getTaskDetailsUrl(baseUrl, task.id))
                    .header(HttpHeaders.AUTHORIZATION, authorization)
                    .awaitExchangeAndThrowError()
                    .awaitBody<TaskDetails>()
        }
        logger.debug { "time taken for moreTaskDetails for $currentUser --->${response.second}" }
        return response.first
    }

    /**
     * fetches the latest comments on the task.
     *
     * @param authorization is the token needed for authorizing the call.
     * @param baseUrl is the endPoint to be called.
     * @param task Task Object.
     * @return List of senderName to Comment Pair.
     */
    suspend fun getLatestCommentsOnTask(
            authorization: String,
            baseUrl: String,
            task: Task,
            currentUser: String?): List<Pair<String, String>>? {
        return task.conversationThreadId?.let {
            val url = Endpoints.getLatestCommentOnTaskUrl(baseUrl, task.groupId, it)
            val response = measureTimeMillisPair {
                client
                        .get()
                        .uri(url)
                        .header(HttpHeaders.AUTHORIZATION, authorization)
                        .awaitExchangeAndThrowError()
                        .awaitBody<Map<String, Any>>()
                        .getListOrException<Map<String, Any>>("value")
                        .map { value ->
                            val body = value.getMapOrException<String, Any>("body")
                            val message = body.getStringOrException("content")
                            val sender = value.getMapOrException<String, Any>("sender")
                            val address = sender.getMapOrException<String, Any>("emailAddress")
                            val senderName = address.getStringOrException("name")
                            senderName to getStringExtractedFromHtml(message)
                        }
                        .filter { comment -> comment.second != "" }
                        .reversed()
            }
            logger.debug { "time taken for getLatestMessagesOnTask For $currentUser----->${response.second}" }
            val comments = response.first
            if (comments.size <= 3) comments else (comments.take(3))
        }
    }

    /**
     * fetches the Label Category Names
     *
     * @param authorization is the token needed for authorizing the call.
     * @param baseUrl is the endPoint to be called.
     * @param planId is the UniqueId of the plan.
     * @returns the map that contains Label Category to the Label Name.
     */
    private suspend fun getLabelCategoriesForPlan(
            authorization: String,
            baseUrl: String,
            planId: String,
            currentUser: String?
    ): Map<String, String?> {
        val url = Endpoints.getLabelCategoriesUrl(baseUrl, planId)
        val response = measureTimeMillisPair {
            client
                    .get()
                    .uri(url)
                    .header(HttpHeaders.AUTHORIZATION, authorization)
                    .awaitExchangeAndThrowError()
                    .awaitBody<Map<String, Any>>()
                    .getMapOrException<String, String>("categoryDescriptions")
        }
        logger.debug { "Time Taken for getLabelCategories for $currentUser --->${response.second}" }
        return response.first
    }

}


