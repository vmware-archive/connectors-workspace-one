package com.vmware.connectors.msPlanner.service

import com.vmware.connectors.msPlanner.config.Endpoints
import com.vmware.connectors.msPlanner.config.Endpoints.getGroupIdsUrl
import com.vmware.connectors.msPlanner.config.Endpoints.getNewConversationThreadUrl
import com.vmware.connectors.msPlanner.config.Endpoints.replyToTaskUrl
import com.vmware.connectors.msPlanner.config.MAX_DUE_DAYS
import com.vmware.connectors.msPlanner.dto.Days
import com.vmware.connectors.msPlanner.dto.Task
import com.vmware.connectors.msPlanner.dto.TaskDetails
import com.vmware.connectors.msPlanner.dto.TaskMetaInfo
import com.vmware.connectors.msPlanner.utils.*
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.awaitBody
import org.springframework.web.reactive.function.client.awaitExchange
import org.springframework.web.reactive.function.client.body
import reactor.core.publisher.Mono
import java.util.*

/**
 * MsPlannerBackendService class
 *
 * @property client: WebClient: library to make async http calls
 */
@Component
class MsPlannerBackendService(@Autowired private val client: WebClient) {

    private val logger = getLogger()

    /**
     * This function will return the user id
     *
     * @param authorization is the token needed for authorizing the call
     * @param baseUrl is the endPoint to be called
     * @returns the user's id
     */
    private suspend fun getUserId(
            authorization: String,
            baseUrl: String
    ): String {
        return client
                .get()
                .uri(Endpoints.getUserIdUrl(baseUrl))
                .header(HttpHeaders.AUTHORIZATION, authorization)
                .awaitExchange()
                .awaitBody<Map<String, Any>>()
                .getStringOrDefault("id")
    }

    /**
     * This function will return the user's Name
     *
     * @param authorization is the token needed for authorizing the call
     * @param id is the user's id
     * @param baseUrl is the endPoint to be called
     * @returns the user's displayName
     */
    suspend fun getUserNameFromUserId(
            authorization: String,
            id: String,
            baseUrl: String
    ): String? {
        val url = Endpoints.getUserNameUrl(baseUrl, id)
        return client
                .get()
                .uri(url)
                .header(HttpHeaders.AUTHORIZATION, authorization)
                .awaitExchange()
                .awaitBody<Map<String, Any>>()
                .getStringOrNull("displayName")
    }

    /**
     * this function will return the user's TimeZone
     *
     * @param authorization is the token needed for authorizing the call
     * @param baseUrl is the endPoint to be called
     * @returns the tomeZone of the User
     */
    suspend fun getUserTimeZone(
            authorization: String,
            baseUrl: String
    ): String? {
        val url = Endpoints.getUserTimeZoneUrl(baseUrl)
        return client
                .get()
                .uri(url)
                .header(HttpHeaders.AUTHORIZATION, authorization)
                .awaitExchange()
//        AndThrowError {
//                    logger.error(it) { "error while getting user's timeZone -> $url with $authorization" }
//                }
                .awaitBody<Map<String, Any>>()
                .getStringOrException("value")
    }

    /**
     * This function will return the id's of the user groups
     *
     * @param authorization is the token needed for authorizing the call
     * @param baseUrl is the endPoint to be called
     * @returns the list of all group ids
     */
    private suspend fun getGroupIds(
            authorization: String,
            baseUrl: String
    ): List<String> {
        val response = client
                .post()
                .uri(getGroupIdsUrl(baseUrl))
                .header(HttpHeaders.AUTHORIZATION, authorization)
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .body(
                        Mono.just(
                                mapOf(
                                        "securityEnabledOnly" to false
                                )
                        )
                )
                .awaitExchange()
                .awaitBody<Map<String, Any>>()
        return response.getListOrException("value")
    }

    /**
     * This function will return the id's of the user group Planners
     *
     * @param groupIds is the list of group ids
     * @param authorization is the token needed for authorizing the call
     * @param baseUrl is the endPoint to be called.
     * @param plans is the list of ids to be returned
     * @returns the list of all group Planner ids
     */
    private suspend fun getPlans(
            groupIds: List<String>,
            authorization: String,
            baseUrl: String,
            plans: List<String> = emptyList()
    ): List<String> {
        return when {
            groupIds.isEmpty() -> plans
            groupIds.size <= 20 ->
                plans.plus(
                        getIds(authorization, baseUrl, getPlanBatchBody(groupIds))
                                .map {
                                    val planId = it.getStringOrException("id")
                                    val groupId = it.getStringOrException("groupId")
                                    val title = it.getStringOrException("title")
                                    "$groupId $planId $title"
                                }
                )
            else -> {
                val batchBody = getPlanBatchBody(groupIds.take(20))
                plans.plus(getIds(authorization, baseUrl, batchBody)
                        .map {
                            val planId = it.getStringOrException("id")
                            val groupId = it.getStringOrException("groupId")
                            val title = it.getStringOrException("title")
                            "$groupId $planId $title"
                        }) +
                        getPlans(groupIds.drop(20), authorization, baseUrl, plans)
            }
        }
    }

    /**
     * This function will return the id's of the Planner Buckets
     *
     * @param planIds is the list of Planner ids
     * @param authorization is the token needed for authorizing the call
     * @param baseUrl is the endPoint to be called.
     * @param buckets is the list of bucket ids to be returned
     * @returns the list of all Planner bucket ids
     */
    private suspend fun getBuckets(
            planIds: List<String>,
            authorization: String,
            baseUrl: String,
            buckets: List<String> = emptyList()
    ): List<String> {
        return when {
            planIds.isEmpty() -> buckets
            planIds.size <= 20 ->
                buckets.plus(getIds(authorization, baseUrl, getBucketBatchBody(planIds), isBucket = true)
                        .map {
                            val bucketId = it.getStringOrException("id")
                            val groupId = it.getStringOrException("groupId")
                            val planName = it.getStringOrException("title")
                            val bucketName = it.getStringOrException("name")
                            TaskMetaInfo(groupId, bucketId, planName, bucketName).serialize()
                        })
            else -> {
                val batchBody = getBucketBatchBody(planIds.take(20))
                buckets.plus(getIds(authorization, baseUrl, batchBody, isBucket = true)
                        .map {
                            val bucketId = it.getStringOrException("id")
                            val groupId = it.getStringOrException("groupId")
                            val planName = it.getStringOrException("title")
                            val bucketName = it.getStringOrException("name")
                            TaskMetaInfo(groupId, bucketId, planName, bucketName).serialize()
                        }) +
                        getPlans(planIds.drop(20), authorization, baseUrl, planIds)
            }
        }
    }

    /**
     * This function will return all the user's tasks in Task Object Format
     *
     * @param authorization is the token needed for authorizing the call
     * @param baseUrl is the endPoint to be called.
     * @returns the list of all the users's tasks
     */
    suspend fun getTasks(
            authorization: String,
            baseUrl: String
    ): List<Task> {
        val userId = getUserId(authorization, baseUrl)
        val groupIds = getGroupIds(authorization, baseUrl)
        val planIds = getPlans(groupIds, authorization, baseUrl)
        val planCategoriesMap = planIds.map { it.split(" ")[1] }.associateWith { getLabelCategories(authorization, baseUrl, it) }
        val bucketIds = getBuckets(planIds, authorization, baseUrl)
        val tasks = getAllTasks(authorization, baseUrl, bucketIds).map {
            val planId = it.getStringOrException("planId")
            it.plus("userId" to userId)
                    .plus("categoryMap" to planCategoriesMap.getValue(planId))
        }
        return tasks.map { it.convertValue<Task>() }
                .filter {
                    it.percentComplete != 100 && it.dueDateTime != null
                            && it.dueDateTime <= formatDateToString(Date().plus(Days(MAX_DUE_DAYS, "UTC")))
                }
    }

    /**
     * This function will make the call to the endPoint and
     * returns responses
     *
     * @param authorization is the token needed for authorizing the call
     * @param baseUrl is the endPoint to be called.
     * @param batchBody is the body of the batch request
     * @returns the list of required ids
     */
    private suspend fun getIds(
            authorization: String,
            baseUrl: String,
            batchBody: List<Map<String, String>>,
            isBucket: Boolean = false,
            isTask: Boolean = false
    ): List<Map<String, Any>> {
        val client = client
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
                .awaitExchange()
                .awaitBody<Map<String, Any>>()
        val responses = client.getListOrException<Map<String, Any>>("responses")
        val body = responses
                .map { (it.getMapOrException<String, Any>("body")).plus("id" to it.getStringOrException("id")) }
        return when {
            isBucket -> body.flatMap {
                val id = it.getStringOrException("id").split(" ")
                val groupId = id.first()
                val title = id.drop(2).joinToString(separator = " ")
                it.getListOrException<Map<String, Any>>("value").map { value ->
                    value.plus(mapOf("groupId" to groupId, "title" to title))
                }
            }
            isTask -> body.flatMap {
                it.getListOrException<Map<String, Any>>("value")
            }
            else -> body.flatMap {
                it.getListOrException<Map<String, Any>>("value").map { value ->
                    value.plus(mapOf("groupId" to it.getValue("id")))
                }
            }
        }
    }

    /**
     * This function will return all the user's tasks
     *
     * @param authorization is the token needed for authorizing the call
     * @param baseUrl is the endPoint to be called.
     * @returns the list of all user's tasks
     */
    private suspend fun getAllTasks(
            authorization: String,
            baseUrl: String,
            bucketIds: List<String>,
            tasks: List<Map<String, Any>> = emptyList()
    ): List<Map<String, Any>> {
        val bucketIdsMap = bucketIds.associate {
            val taskMetaInfo = it.deserialize<TaskMetaInfo>()
            taskMetaInfo.bucketId to taskMetaInfo
        }
        return when {
            bucketIds.isEmpty() -> tasks
            bucketIds.size <= 20 ->
                tasks.plus(getIds(authorization, baseUrl, getTaskBatchBody(bucketIds), isBucket = false, isTask = true).map {
                    it.plus("taskMetaInfo" to bucketIdsMap.getValue(it.getStringOrException("bucketId")))
                })
            else -> {
                val batchBody = getTaskBatchBody(bucketIds.take(20))
                tasks.plus(getIds(authorization, baseUrl, batchBody).map {
                    it.plus("taskMetaInfo" to bucketIdsMap.getValue(it.getStringOrException("bucketId")))
                }) +
                        getAllTasks(authorization, baseUrl, bucketIds.drop(20), tasks)
            }
        }
    }

    /**
     * returns the user Tasks
     * @param tasks list of user's tasks
     * @returns the pair of userId to list of user's tasks
     */
    fun getUserTasks(
            tasks: List<Task>
    ): Pair<String?, List<Task>> {
        val filteredTasks = tasks
                .filter {
                    it.assignments.keys.contains(it.userId) &&
                            formatDateWithOutTime(formatStringToDate(it.dueDateTime)) == formatDateWithOutTime(Date())
                }
                .sortedBy { it.dueDateTime }
        val userId = tasks.firstOrNull()?.userId
        return userId to filteredTasks
    }

    /**
     * updates the task as completed.
     * @param authorization is the token needed for authorizing the call
     * @param baseUrl is the endPoint to be called.
     * @param task is the Task Object.
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
     * updates the task's conversationId property.
     * @param authorization is the token needed for authorizing the call
     * @param baseUrl is the endPoint to be called.
     * @param task is the Task Object.
     * @param comment is the first message to be posted.
     */
    private suspend fun updateConversationId(
            authorization: String,
            baseUrl: String,
            task: Task,
            comment: String
    ): Boolean {
        val body = mapOf("conversationThreadId" to
                getNewConversationThread(
                        authorization, baseUrl, task.groupId, task.title, comment
                ))
        return updateTask(authorization, baseUrl, task, body)
    }

    /**
     * updates the given task.
     * @param authorization is the token needed for authorizing the call
     * @param baseUrl is the endPoint to be called.
     * @param task is the Task Object to be updated
     * @param body of the post request.
     */
    private suspend fun updateTask(
            authorization: String,
            baseUrl: String,
            task: Task,
            body: Map<String, Any>
    ): Boolean {
        val eTag = task.eTag
        val url = Endpoints.updateTaskUrl(baseUrl, task.id)
        return client
                .patch()
                .uri(url)
                .header(HttpHeaders.AUTHORIZATION, authorization)
                .header(HttpHeaders.IF_MATCH, eTag)
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .body(Mono.just(body))
                .awaitExchange()
                .statusCode()
                .is2xxSuccessful
    }

    /**
     *
     * this function replies a message to the task
     * @param authorization is the token needed for authorizing the call
     * @param baseUrl is the endPoint to be called.
     * @param task is the Task Object to be updated
     * @param message reply text to be sent
     */
    suspend fun addCommentsToTask(
            authorization: String,
            baseUrl: String,
            task: Task,
            message: String?
    ): Boolean {
        logger.info { "conversationThreadId -> ${task}, message -> $message" }
        return if (task.conversationThreadId != null && message != null) {
            val url = replyToTaskUrl(baseUrl, task.groupId, task.conversationThreadId)
            client
                    .post()
                    .uri(url)
                    .header(HttpHeaders.AUTHORIZATION, authorization)
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                    .body(Mono.just(getAddCommentsToTaskBody(message)))
                    .awaitExchange()
                    .statusCode()
                    .is2xxSuccessful
        } else {
            if (message != null) {
                val updatedTask = getTaskById(baseUrl, authorization, task.id, task.groupId)
                if (updatedTask.conversationThreadId == null) {
                    updateConversationId(authorization, baseUrl, task, message)
                } else addCommentsToTask(authorization, baseUrl, updatedTask, message)
            } else false
        }
    }

    /**
     * creates the new conversation and returns the New ConversationThread Id.
     * @param authorization is the token needed for authorizing the call
     * @param baseUrl is the endPoint to be called.
     * @param taskName is the Name of the Task.
     * @param comment is the first message to be posted
     * @returns the New ConversationThread Id.
     */
    private suspend fun getNewConversationThread(
            authorization: String,
            baseUrl: String,
            groupId: String,
            taskName: String,
            comment: String
    ): String {
        val url = getNewConversationThreadUrl(baseUrl, groupId)
        return client
                .post()
                .uri(url)
                .header(HttpHeaders.AUTHORIZATION, authorization)
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .body(Mono.just(getConversationThreadBody(taskName, comment)))
                .awaitExchange()
                .awaitBody<Map<String, Any>>()
                .getListOrException<Map<String, Any>>("threads").first()
                .getStringOrException("id")
    }

    /**
     * this function returns the Task object.
     *
     * @param baseUrl is the endPoint to be called.
     * @param authorization is the token needed for authorizing the call
     * @param taskId is the Id of the Task.
     * @param groupId is the Id of the Group to which the task belongs to.
     * @returns the Task Object.
     */
    private suspend fun getTaskById(
            baseUrl: String,
            authorization: String,
            taskId: String,
            groupId: String): Task {
        val url = Endpoints.getTaskByIdUrl(baseUrl, taskId)
        val task = client.get()
                .uri(url)
                .header(HttpHeaders.AUTHORIZATION, authorization)
                .awaitExchange()
                .awaitBody<Map<String, Any>>()
                .plus(mapOf("groupId" to groupId))
        return task.convertValue()
    }

    /**
     * gets userPrincipalName(email) from the user office365 profile
     * @param authorization: authorization OAuth token
     * @return user emailId
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
     * gets the emaiId from the Vmware user token
     */
    fun getUserEmailFromToken(token: String): String? {
        val pl = token
                .split(" ")
                .lastOrNull()
                ?.split(".")
                ?.get(1)
                ?.toBase64DecodedString()

        return pl?.deserialize<Map<String, Any>>()
                ?.getStringOrNull("eml")
    }

    /**
     * returns details of the task
     *
     * @receiver Task object
     * @param authorization is the token needed for authorizing the call
     * @param baseUrl is the endPoint to be called.
     * @returns TaskDetails object
     */
    suspend fun getMoreTaskDetails(task: Task, authorization: String, baseUrl: String): TaskDetails {
        return client
                .get()
                .uri(Endpoints.getTaskDetailsUrl(baseUrl, task.id))
                .header(HttpHeaders.AUTHORIZATION, authorization)
                .awaitExchange()
                .awaitBody()
    }

    /**
     * this fumction will returns the latest comment on the task.
     *
     * @param task is the Task object
     * @param authorization is the token needed for authorizing the call
     * @param baseUrl is the endPoint to be called.
     * @returns the latest comment of the task.
     */
    suspend fun getLatestCommentOnTask(
            task: Task,
            authorization: String,
            baseUrl: String): String? {
        return task.conversationThreadId?.let {
            val url = Endpoints.getLatestCommentOnTaskUrl(baseUrl, task.groupId, it)
            val comments = client
                    .get()
                    .uri(url)
                    .header(HttpHeaders.AUTHORIZATION, authorization)
                    .awaitExchange()
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
            return if (comments.size <= 3) generateHtmlFromComments(comments)
            else generateHtmlFromComments(comments.take(3))
        }
    }

    /**
     * this function returns the CategoryNames Map
     *
     * @param authorization is the token needed for authorizing the call
     * @param baseUrl is the endPoint to be called
     * @param planId is the UniqueId of the plan
     * @returns the CategoriesMap
     */
    private suspend fun getLabelCategories(
            authorization: String,
            baseUrl: String,
            planId: String
    ): Map<String, String?> {
        val url = Endpoints.getLabelCategoriesUrl(baseUrl, planId)
        return client
                .get()
                .uri(url)
                .header(HttpHeaders.AUTHORIZATION, authorization)
                .awaitExchange()
                .awaitBody<Map<String, Any>>()
                .getMapOrException("categoryDescriptions")
    }

}


